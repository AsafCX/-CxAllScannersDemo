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
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String BASE_URL = "https://gitlab.com/api/v4";

    private static final String URL_GET_NAMESPACES = BASE_URL + "/namespaces/";

    private static final String URL_GET_REPOS = BASE_URL + "/projects?simple=true&membership=true";

    private static final String GITLAB_BASE_URL = "gitlab.com";

    private static final String SCOPES ="api";

    private static final String URL_GET_WEBHOOKS = BASE_URL + "/projects/%s/hooks";

    private static final String URL_DELETE_WEBHOOK = BASE_URL + "/projects/%s/hooks/%s";

    private static final String URL_CREATE_WEBHOOK = BASE_URL + "/projects/%s/hooks?url=%s&token=%s&merge_requests_events=true&push_events=true";

    private static final String TOKEN_REQUEST_USER_AGENT = "CxIntegrations";

    @Value("${redirect.url}")
    private String redirectUrl;
    

    @Override
    protected Map<String, String> getHeaders() {
        // If we don't specify any User-Agent, the request will fail with "403 Forbidden: [error code: 1010]".
        // This issue may not exist in some execution environments.
        return Collections.singletonMap(HttpHeaders.USER_AGENT, GitLabService.TOKEN_REQUEST_USER_AGENT);
    }
    
    @Override
    protected String getPath(String oAuthCode, ScmDto scmDto) {
        return String.format(GitLabService.URL_AUTH_TOKEN, scmDto.getClientId(),
                scmDto.getClientSecret(),
                oAuthCode,
                redirectUrl);
    }
    
    @Override
    public String getScopes() {
        return SCOPES;
    }

    public String getBaseUrl() {
        return GITLAB_BASE_URL;
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
                Converter.convertToListOrgAccessToken(accessToken, organizationWebDtos, getBaseUrl());
        dataStoreService.storeScmOrgsToken(scmAccessTokenDtos);
        return organizationWebDtos;
    }


    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseUrl(), orgId);
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
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseUrl(), orgId);
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
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseUrl(), orgId);
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
