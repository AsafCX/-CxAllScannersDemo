package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.gitlab.AccessTokenGitlabDto;
import com.checkmarx.dto.gitlab.RepoGitlabDto;
import com.checkmarx.dto.gitlab.WebhookGitLabDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;


@Slf4j
@Service("gitlab")
public class GitLabService implements ScmService  {

    private static final String URL_AUTH_TOKEN = "https://gitlab.com/oauth/token";

    private static final String URL_REFRESH_TOKEN = URL_AUTH_TOKEN + "?grant_type=%s" +
            "&refresh_token=%s&client_id=%s&client_secret=%s";

    private static final String URL_GENERATE_TOKEN = URL_AUTH_TOKEN + "?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s";

    private static final String BASE_API_URL = "https://gitlab.com/api/v4";

    private static final String URL_GET_GROUPS = BASE_API_URL + "/groups?top_level_only=true";

    private static final String URL_GET_PROJECTS = BASE_API_URL + "/groups/%s/projects" +
            "?include_subgroups=true";

    private static final String BASE_DB_KEY = "gitlab.com";

    private static final String SCOPES ="api";

    private static final String GRANT_TYPE ="refresh_token";

    private static final String URL_GET_WEBHOOKS = BASE_API_URL + "/projects/%s/hooks";

    private static final String URL_DELETE_WEBHOOK = BASE_API_URL + "/projects/%s/hooks/%s";

    private static final String URL_CREATE_WEBHOOK = BASE_API_URL + "/projects/%s/hooks?url=%s&token=%s&merge_requests_events=true&push_events=true";

    private static final String URL_VALIDATE_TOKEN = "https://gitlab.com/api/v4/user";

    private static final String TOKEN_REQUEST_USER_AGENT = "CxIntegrations";

    @Value("${gitlab.redirect.url}")
    private String gitlabRedirectUrl;

    @Value("${cxflow.webhook.url}")
    protected String cxFlowWebHook;

    @Autowired
    RestWrapper restWrapper;

    @Autowired
    DataService dataStoreService;
    
    @Override
    public String getScopes() {
        return SCOPES;
    }

    public String getBaseDbKey() {
        return BASE_DB_KEY;
    }

    @Override
    public List<OrganizationWebDto> getOrganizations(@NonNull String authCode) {
        AccessTokenGitlabDto accessToken = generateAccessToken(authCode);
        log.info("Access token generated successfully");
        return getAndStoreOrganizations(accessToken);
    }


    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        AccessTokenGitlabDto token = getGitlabOrgToken(scmAccessTokenDto.getAccessToken());
        String path = String.format(URL_GET_PROJECTS) ;
        ResponseEntity<RepoGitlabDto[]> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
                                       null, null,
                                       RepoGitlabDto[].class, token.getAccessToken());
        ArrayList<RepoGitlabDto> repoGitlabDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (RepoGitlabDto repoGitlabDto : repoGitlabDtos) {
            WebhookGitLabDto webhookGitlabDto = getRepositoryCxFlowWebhook(repoGitlabDto.getId(),
                                                                           token.getAccessToken());
            if(webhookGitlabDto != null) {
                repoGitlabDto.setWebHookEnabled(true);
                repoGitlabDto.setWebhookId(webhookGitlabDto.getId());
            } else {
                repoGitlabDto.setWebHookEnabled(false);
            }
            repoGitlabDto.setName(StringUtils.substringAfter(repoGitlabDto.getName(), "/"));
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(scmAccessTokenDto, repoGitlabDtos);
        dataStoreService.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(repoGitlabDtos);
    }

    private AccessTokenGitlabDto getGitlabOrgToken(String tokenObj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(tokenObj, AccessTokenGitlabDto.class);
        } catch (JsonProcessingException ex){
            throw new ScmException("Unable to Json -> Object");
        }
    }


    @Override
    public String createWebhook(@NonNull String orgId, @NonNull String projectId ) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        AccessTokenGitlabDto token = getGitlabOrgToken(scmAccessTokenDto.getAccessToken());
        String path = String.format(URL_CREATE_WEBHOOK, projectId, cxFlowWebHook, "1234") ;
         ResponseEntity<WebhookGitLabDto> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.POST,
                                                                                        new WebhookGitLabDto(), null,
                                                                                        WebhookGitLabDto.class,
                                                                                        token.getAccessToken());
        WebhookGitLabDto webhookGitLabDto = response.getBody();
        validateResponse(webhookGitLabDto);
        dataStoreService.updateWebhook(projectId, scmAccessTokenDto, webhookGitLabDto.getId(), true);
        return webhookGitLabDto.getId();
    }

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String webhookId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        AccessTokenGitlabDto token = getGitlabOrgToken(scmAccessTokenDto.getAccessToken());
        String path = String.format(URL_DELETE_WEBHOOK, repoId, webhookId);

        try {
            restWrapper.sendBearerAuthRequest(path, HttpMethod.DELETE,null, null,
                                              WebhookGitLabDto.class,
                                              token.getAccessToken());
        } catch (HttpClientErrorException ex){
            if(ex.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                log.error("Webhook not found: {}", ex.getMessage());
                throw new ScmException(RestWrapper.WEBHOOK_DELETE_FAILURE);
            }
            throw new ScmException(RestWrapper.GENERAL_RUNTIME_EXCEPTION);
        }
        dataStoreService.updateWebhook(repoId, scmAccessTokenDto, null, false);
    }
    

    @Override
    public CxFlowConfigDto getCxFlowConfiguration(@NonNull String orgId) {
        //CxFlow send org name, Using DataStore to get org id
        OrgDto orgDto = dataStoreService.getScmOrgByName(getBaseDbKey(), orgId);
        CxFlowConfigDto cxFlowConfigDto = buildCxFlowConfig(orgDto);
        return validateCxFlowConfig(cxFlowConfigDto, getGitlabOrgToken(orgDto.getAccessToken()));

    }

    private List<OrganizationWebDto> getAndStoreOrganizations(AccessTokenGitlabDto token) {
        ResponseEntity<OrganizationWebDto[]> response =
                restWrapper.sendBearerAuthRequest(URL_GET_GROUPS, HttpMethod.GET, null, null,
                                                  OrganizationWebDto[].class, token.getAccessToken());
        List<OrganizationWebDto> organizationWebDtos = new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
        String tokenJson = Converter.convertObjectToJson(token);
        List<OrgDto> orgDtos =
                Converter.convertToListOrg(tokenJson, organizationWebDtos, getBaseDbKey());
        dataStoreService.storeOrgs(orgDtos);

        return organizationWebDtos;
    }

    private CxFlowConfigDto validateCxFlowConfig(CxFlowConfigDto cxFlowConfigDto, AccessTokenGitlabDto token) {
        if(StringUtils.isEmpty(cxFlowConfigDto.getScmAccessToken()) || StringUtils.isEmpty(cxFlowConfigDto.getTeam()) || StringUtils.isEmpty(cxFlowConfigDto.getCxgoSecret())) {
            log.error("CxFlow configuration settings validation failure, missing data");
            throw new ScmException("CxFlow configuration settings validation failure, missing data");
        }
        try {
            restWrapper.sendBearerAuthRequest(URL_VALIDATE_TOKEN, HttpMethod.GET, null, null,
                                              CxFlowConfigDto.class,
                                              cxFlowConfigDto.getScmAccessToken());
            log.info("Gitlab token validation passed successfully!");
        } catch (HttpClientErrorException ex){
            token = refreshToken(token);
            cxFlowConfigDto.setScmAccessToken(token.getAccessToken());
            log.info("Gitlab refresh token process passed successfully!");
        }
        return cxFlowConfigDto;
    }

    private AccessTokenGitlabDto refreshToken(AccessTokenGitlabDto token) {
        token = sendRefreshTokenRequest(token.getRefreshToken());
        getAndStoreOrganizations(token);
        return token;
    }

    private CxFlowConfigDto buildCxFlowConfig(OrgDto orgDto) {
        AccessTokenGitlabDto token = getGitlabOrgToken(orgDto.getAccessToken());
        OrgPropertiesDto orgPropertiesDto = dataStoreService.getScmOrgSettings(getBaseDbKey(),
                                                                               orgDto.getOrgIdentity());
        return CxFlowConfigDto.builder()
                .team(orgPropertiesDto.getCxTeam())
                .cxgoSecret(orgPropertiesDto.getCxGoToken())
                .scmAccessToken(token.getAccessToken())
                .build();
    }

    private AccessTokenGitlabDto sendRefreshTokenRequest(String refreshToken) {
        String path = buildRefreshTokenPath(refreshToken);
        return sendAccessTokenRequest(path, getHeadersAccessToken());
    }

    private String buildRefreshTokenPath(String refreshToken) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        return String.format(URL_REFRESH_TOKEN, GRANT_TYPE, refreshToken,
                             scmDto.getClientId(), scmDto.getClientSecret());
    }

    private WebhookGitLabDto getRepositoryCxFlowWebhook(@NonNull String repoId,
                                                        @NonNull String accessToken){
        String path = String.format(URL_GET_WEBHOOKS, repoId);
        ResponseEntity<WebhookGitLabDto[]> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                WebhookGitLabDto[].class, accessToken);
        ArrayList<WebhookGitLabDto> webhookGitLabDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (WebhookGitLabDto webhookGitLabDto : webhookGitLabDtos) {
            if (webhookGitLabDto != null  && webhookGitLabDto.getUrl().equals(cxFlowWebHook))
                return webhookGitLabDto;
        }
        return null;
    }

    private void validateResponse(WebhookGitLabDto webhookGitLabDto) {
        if(webhookGitLabDto == null || StringUtils.isEmpty(webhookGitLabDto.getId())){
            log.error(RestWrapper.WEBHOOK_CREATE_FAILURE);
            throw new ScmException(RestWrapper.WEBHOOK_CREATE_FAILURE);
        }
    }

    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitHub api
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitHub
     */
    private AccessTokenGitlabDto generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        String path = buildPathAccessToken(oAuthCode, scmDto);
        return sendAccessTokenRequest(path, getHeadersAccessToken());
    }

    private AccessTokenGitlabDto sendAccessTokenRequest(String path, Map<String, String> headers) {
        ResponseEntity<AccessTokenGitlabDto> response = restWrapper.sendRequest(path, HttpMethod.POST, null, headers, AccessTokenGitlabDto.class);
        AccessTokenGitlabDto tokenGitlabDto = response.getBody();
        if(!verifyAccessToken(tokenGitlabDto)){
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return tokenGitlabDto;
    }

    private Map<String, String> getHeadersAccessToken() {
        // If we don't specify any User-Agent, the request will fail with "403 Forbidden: [error code: 1010]".
        // This issue may not exist in some execution environments.
        return Collections.singletonMap(HttpHeaders.USER_AGENT, GitLabService.TOKEN_REQUEST_USER_AGENT);
    }

    private String buildPathAccessToken(String oAuthCode, ScmDto scmDto) {
        return String.format(GitLabService.URL_GENERATE_TOKEN, scmDto.getClientId(),
                             scmDto.getClientSecret(),
                             oAuthCode,
                             gitlabRedirectUrl);
    }

    /**
     * verifyAccessToken method used to verify access token creation, Currently checks if access
     * token created(not null or empty) without GitHub validation
     *
     * @param accessToken access token generated before using GitHub api, Gives access to relevant
     *                  GitHub data
     * @return true if verification passed successfully
     */
    private boolean verifyAccessToken(AccessTokenGitlabDto accessToken) {
        return accessToken != null && accessToken.getAccessToken() != null && !accessToken.getAccessToken().isEmpty();
    }


}
