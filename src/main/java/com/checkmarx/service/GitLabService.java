package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.IRepoDto;
import com.checkmarx.dto.gitlab.RepoGitlabDto;
import com.checkmarx.dto.gitlab.WebhookGitLabDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service("gitlab")
public class GitLabService extends AbstractScmService implements ScmService  {

    private static final String URL_AUTH_TOKEN = "https://gitlab.com/oauth/token?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s";

    private static final String BASE_API_URL = "https://gitlab.com/api/v4";

    private static final String URL_GET_NAMESPACES = BASE_API_URL + "/namespaces/";

    private static final String URL_GET_REPOS = BASE_API_URL + "/projects?simple=true&membership=true";

    private static final String BASE_DB_KEY = "gitlab.com";

    private static final String SCOPES ="api";

    private static final String URL_GET_WEBHOOKS = BASE_API_URL + "/projects/%s/hooks";

    private static final String URL_DELETE_WEBHOOK = BASE_API_URL + "/projects/%s/hooks/%s";

    private static final String URL_CREATE_WEBHOOK = BASE_API_URL + "/projects/%s/hooks?url=%s&token=%s&merge_requests_events=true&push_events=true";

    private static final String TOKEN_REQUEST_USER_AGENT = "CxIntegrations";

    @Value("${gitlab.redirect.url}")
    private String gitlabRedirectUrl;

    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitHub api
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitHub
     */
    protected AccessTokenDto generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        String path = buildPathAccessToken(oAuthCode, scmDto);
        ResponseEntity<AccessTokenDto> response = generateAccessToken(restWrapper, path, getHeadersAccessToken(), null);
        return response.getBody();
    }
    
    protected ResponseEntity<AccessTokenDto> sendAccessTokenRequest(RestWrapper restWrapper, String path, Map<String, String> headers, Object body) {
        return (ResponseEntity<AccessTokenDto>) restWrapper.sendRequest(path, HttpMethod.POST,
                body, headers,
                AccessTokenDto.class);
    }
    
    public ResponseEntity<AccessTokenDto> generateAccessToken(RestWrapper restWrapper, String path, Map<String, String> headers, Object body) {
        ResponseEntity<AccessTokenDto> response = sendAccessTokenRequest(restWrapper, path, headers, body);

        if(!verifyAccessToken(response.getBody())){
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return response;
    }
    
    protected Map<String, String> getHeadersAccessToken() {
        // If we don't specify any User-Agent, the request will fail with "403 Forbidden: [error code: 1010]".
        // This issue may not exist in some execution environments.
        return Collections.singletonMap(HttpHeaders.USER_AGENT, GitLabService.TOKEN_REQUEST_USER_AGENT);
    }
    

    protected String buildPathAccessToken(String oAuthCode, ScmDto scmDto) {
        return String.format(GitLabService.URL_AUTH_TOKEN, scmDto.getClientId(),
                scmDto.getClientSecret(),
                oAuthCode,
                gitlabRedirectUrl);
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
        AccessTokenDto accessToken = generateAccessToken(authCode);
        log.info("Access token generated successfully");

        ResponseEntity<OrganizationWebDto[]> response =
                restWrapper.sendBearerAuthRequest(URL_GET_NAMESPACES, HttpMethod.GET, null, null,
                        OrganizationWebDto[].class, accessToken.getAccessToken());
        List<OrganizationWebDto> organizationWebDtos = new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
        List<ScmAccessTokenDto> scmAccessTokenDtos =
                Converter.convertToListOrgAccessToken(accessToken, organizationWebDtos, getBaseDbKey());
        dataStoreService.storeScmOrgsToken(scmAccessTokenDtos);
        return organizationWebDtos;
    }


    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        String path = String.format(URL_GET_REPOS, orgId);
        ResponseEntity<RepoGitlabDto[]> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
                                       null, null,
                                       RepoGitlabDto[].class, scmAccessTokenDto.getAccessToken());
        ArrayList<RepoGitlabDto> repoGitlabDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        List<? extends IRepoDto> filteredRepos = repoGitlabDtos.stream().filter(repoGitlabDto -> repoGitlabDto.getNamespace().getId().equals(orgId)).collect(Collectors.toList());
        for (IRepoDto repoGitlabDto : filteredRepos) {
            WebhookGitLabDto webhookGitlabDto = getRepositoryCxFlowWebhook(repoGitlabDto.getId(),
                    scmAccessTokenDto.getAccessToken());
            if(webhookGitlabDto != null) {
                repoGitlabDto.setWebHookEnabled(true);
                repoGitlabDto.setWebhookId(webhookGitlabDto.getId());
            } else {
                repoGitlabDto.setWebHookEnabled(false);
            }
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(scmAccessTokenDto, filteredRepos);
        dataStoreService.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(filteredRepos);
    }


    @Override
    public String createWebhook(@NonNull String orgId, @NonNull String projectId ) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        String path = String.format(URL_CREATE_WEBHOOK, projectId, cxFlowWebHook, scmAccessTokenDto.getAccessToken()) ;
         ResponseEntity<WebhookGitLabDto> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.POST,
                 new WebhookGitLabDto(), null,
                 WebhookGitLabDto.class,
                scmAccessTokenDto.getAccessToken());
        WebhookGitLabDto webhookGitLabDto = response.getBody();
        validateResponse(webhookGitLabDto);
        dataStoreService.updateWebhook(projectId, scmAccessTokenDto, webhookGitLabDto.getId(), true);
        return webhookGitLabDto.getId();
    }

    private void validateResponse(WebhookGitLabDto webhookGitLabDto) {
        if(webhookGitLabDto == null || StringUtils.isEmpty(webhookGitLabDto.getId())){
            log.error(RestWrapper.WEBHOOK_CREATE_FAILURE);
            throw new ScmException(RestWrapper.WEBHOOK_CREATE_FAILURE);
        }
    }

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String webhookId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        String path = String.format(URL_DELETE_WEBHOOK, repoId, webhookId);

        try {
            restWrapper.sendBearerAuthRequest(path, HttpMethod.DELETE,null, null,
                    WebhookGitLabDto.class,
                    scmAccessTokenDto.getAccessToken());
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
    public CxFlowConfigDto validateCxFlowConfiguration(@NonNull CxFlowConfigDto cxFlowConfigDto) {
        //TODO
        return null;
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


}
