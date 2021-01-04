package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.BaseDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.gitlab.*;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service("gitlab")
public class GitLabService extends AbstractScmService implements ScmService  {

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

    private static final String URL_WEBHOOK = BASE_API_URL + "/projects/%s/hooks?url=%s&token=%s&merge_requests_events=true&push_events=true";

    private static final String URL_VALIDATE_TOKEN = "https://gitlab.com/api/v4/user";

    private static final String TOKEN_REQUEST_USER_AGENT = "CxIntegrations";

    public GitLabService(RestWrapper restWrapper, DataService dataStoreService) {
        super(restWrapper, dataStoreService);
    }


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
        AccessTokenManager accessTokenManager = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);

        String path = String.format(URL_GET_PROJECTS, orgId);
        ResponseEntity<RepoGitlabDto[]> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
                                       null, null,
                                       RepoGitlabDto[].class, accessTokenManager.getAccessTokenStr());
        ArrayList<RepoGitlabDto> repoGitlabDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (RepoGitlabDto repoDto : repoGitlabDtos) {
            WebhookGitLabDto webhookDto = getRepositoryCxFlowWebhook(repoDto.getId(),
                    accessTokenManager.getAccessTokenStr());
            setWebhookDetails(repoDto, webhookDto);
            repoDto.setName(StringUtils.substringAfter(repoDto.getName(), "/"));
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(accessTokenManager.getDbDto(), repoGitlabDtos);
        dataStoreService.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(repoGitlabDtos);
    }

    @Override
    public BaseDto createWebhook(@NonNull String orgId, @NonNull String projectId ) {
        AccessTokenManager accessTokenManager = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);

        String path = String.format(URL_WEBHOOK, projectId, getCxFlowUrl(), "1234") ;
         ResponseEntity<WebhookGitLabDto> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.POST,
                                                                                        new WebhookGitLabDto(), null,
                                                                                        WebhookGitLabDto.class,
                                                                                        accessTokenManager.getAccessTokenStr());
        WebhookGitLabDto webhookGitLabDto = Objects.requireNonNull(
                response.getBody(), "Missing webhook creation response.") ;

        validateWebhookDto(webhookGitLabDto);
        dataStoreService.updateWebhook(projectId, accessTokenManager.getDbDto(), webhookGitLabDto.getId(), true);
        return new BaseDto(webhookGitLabDto.getId());
    }

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String deleteUrl) {
        String path = String.format(URL_DELETE_WEBHOOK, repoId, deleteUrl);
        super.deleteWebhook(orgId,repoId,path,WebhookGitLabDto.class);
    }

    @Override
    public CxFlowConfigDto getCxFlowConfiguration(@NonNull String orgId) {
        AccessTokenManager tokenManager = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);
        CxFlowConfigDto result = getOrganizationSettings(orgId, tokenManager.getAccessTokenStr());
        Object tokenDto  = tokenManager.getFullAccessToken(AccessTokenGitlabDto.class);
        validateCxFlowConfig(result, (AccessTokenGitlabDto)tokenDto);
        return result;
    }

    private List<OrganizationWebDto> getAndStoreOrganizations(AccessTokenGitlabDto tokenResponse) {
        List<GroupGitlabDto> groups = getUserGroups(tokenResponse.getAccessToken());

        String tokenResponseJson = AccessTokenManager.convertObjectToJson(tokenResponse);

        List<OrgDto> dataStoreOrgs = toDataStoreOrganizations(groups, tokenResponseJson);
        dataStoreService.storeOrgs(dataStoreOrgs);

        return toOrganizationsForWebClient(groups);
    }

    private List<GroupGitlabDto> getUserGroups(String accessToken) {
        ResponseEntity<GroupGitlabDto[]> response =
                restWrapper.sendBearerAuthRequest(URL_GET_GROUPS, HttpMethod.GET, null, null,
                                                  GroupGitlabDto[].class, accessToken);

        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    private List<OrganizationWebDto> toOrganizationsForWebClient(List<GroupGitlabDto> gitlabOrgs) {
        return gitlabOrgs.stream()
                .map(gitlabOrg -> OrganizationWebDto.builder()
                        .id(gitlabOrg.getPath())
                        .name(gitlabOrg.getName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<OrgDto> toDataStoreOrganizations(List<GroupGitlabDto> gitlabOrgs, String tokenJson) {
        return gitlabOrgs.stream()
                .map(gitlabOrg -> OrgDto.builder()
                        .accessToken(tokenJson)
                        .orgIdentity(gitlabOrg.getPath())
                        .scmUrl(getBaseDbKey())
                        .tokenType(TokenType.ACCESS.getType())
                        .build())
                .collect(Collectors.toList());
    }

    private void validateCxFlowConfig(CxFlowConfigDto cxFlowConfig, AccessTokenGitlabDto tokenDto) {
        validateFieldsArePresent(cxFlowConfig);

        if (!accessTokenIsValid(cxFlowConfig.getScmAccessToken())) {
            AccessTokenGitlabDto newTokenDto = refreshToken(tokenDto);
            cxFlowConfig.setScmAccessToken(newTokenDto.getAccessToken());
        }
    }

    private boolean accessTokenIsValid(String token) {
        boolean result = false;
        try {
            restWrapper.sendBearerAuthRequest(URL_VALIDATE_TOKEN, HttpMethod.GET, null, null,
                    CxFlowConfigDto.class,
                    token);
            result =true;
            log.info("Gitlab token validation passed successfully!");
        } catch (HttpClientErrorException ex){
            log.info("Current token is not valid.");
        }
        return result;
    }

    private void validateFieldsArePresent(CxFlowConfigDto cxFlowConfigDto) {
        if(StringUtils.isAnyEmpty(cxFlowConfigDto.getScmAccessToken(), cxFlowConfigDto.getTeam(),
                                  cxFlowConfigDto.getCxgoToken())) {
            log.error("CxFlow configuration settings validation failure, missing data");
            throw new ScmException("CxFlow configuration settings validation failure, missing data");
        }
    }

    private AccessTokenGitlabDto refreshToken(AccessTokenGitlabDto token) {
        token = sendRefreshTokenRequest(token.getRefreshToken());
        getAndStoreOrganizations(token);
        return token;
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
        ArrayList<WebhookGitLabDto> webhookDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));

        return (WebhookGitLabDto) getActiveHook(webhookDtos);
    }


    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitLab api
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitLab
     */
    private AccessTokenGitlabDto generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        String path = buildPathAccessToken(oAuthCode, scmDto);
        return sendAccessTokenRequest(path, getHeadersAccessToken());
    }

    private AccessTokenGitlabDto sendAccessTokenRequest(String path, Map<String, String> headers) {
        ResponseEntity<AccessTokenGitlabDto> response = restWrapper.sendRequest(path, HttpMethod.POST, null, headers, AccessTokenGitlabDto.class);

        AccessTokenGitlabDto tokenGitlabDto = Objects.requireNonNull(
                response.getBody(), "Missing access token generation response.");

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
                             getRedirectUrl());
    }


}
