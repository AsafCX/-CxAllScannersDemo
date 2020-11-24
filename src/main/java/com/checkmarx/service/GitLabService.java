package com.checkmarx.service;

import com.checkmarx.controller.DataController;
import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.github.WebhookGithubDto;
import com.checkmarx.dto.gitlab.AccessTokenGitlabDto;
import com.checkmarx.dto.gitlab.RepoGitlabDto;
import com.checkmarx.dto.gitlab.WebhookGitLabDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class GitLabService implements ScmService  {


    private static String URL_AUTH_TOKEN = "https://gitlab.com/oauth/token?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s";

    private static String BASE_URL = "https://gitlab.com/api/v4";

    private static String URL_GET_NAMESPACES = BASE_URL + "/namespaces/";

    private static String URL_GET_REPOS = BASE_URL + "/projects?simple=true&membership=true";

    private static String GITLAB_BASE_URL = "gitlab.com";

    private static String SCOPES ="api";

    private String URL_GET_WEBHOOKS = BASE_URL + "/projects/%s/hooks";

    private static final String URL_DELETE_WEBHOOK = BASE_URL + "/projects/%s/hooks/%s";

    private static final String URL_CREATE_WEBHOOK = BASE_URL + "/projects/%s/hooks?url=%s&token=%s";
    
    @Value("${redirect.url}")
    private String redirectUrl;

    @Autowired
    RestHelper restHelper;

    @Autowired
    DataController dataStoreController;

    @Value("${cxflow.webhook.url}")
    private String cxFlowWebHook;
    
    
    @Override
    public String getScopes() {
        return SCOPES;
    }

    public String getBaseUrl() {
        return GITLAB_BASE_URL;
    }

    @Override
    public List<OrganizationWebDto> getOrganizations(@NonNull String authCode) {
        AccessTokenGitlabDto accessToken = generateAccessToken(authCode);
        log.info("Access token generated successfully");

        ResponseEntity<OrganizationWebDto[]> response =
                restHelper.sendBearerAuthRequest(URL_GET_NAMESPACES, HttpMethod.GET, null, null,
                        OrganizationWebDto[].class, accessToken.getAccessToken());
        List<OrganizationWebDto> organizationWebDtos = new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
        List<ScmAccessTokenDto> scmAccessTokenDtos =
                Converter.convertToListGitlabOrgAccessToken(accessToken, organizationWebDtos, getBaseUrl());
        dataStoreController.storeScmOrgsToken(scmAccessTokenDtos);
        return organizationWebDtos;
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
        ScmDto scmDto = dataStoreController.getScm(getBaseUrl());
        String path = String.format(URL_AUTH_TOKEN, scmDto.getClientId(),
                scmDto.getClientSecret(),
                oAuthCode,
                redirectUrl);
        ResponseEntity<AccessTokenGitlabDto> response =  restHelper.sendRequest(path, HttpMethod.POST,
                null, null,
                AccessTokenGitlabDto.class);
        if(!verifyAccessToken(response.getBody())){
            log.error(RestHelper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestHelper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return response.getBody();
    }

    /**
     * verifyAccessToken method used to verify access token creation, Currently checks if access
     * token created(not null or empty) without GitHub validation
     *
     * @param accessToken access token generated before using GitHub api, Gives access to relevant
     *                  GitHub data
     * @return true if verification passed successfully
     */
    protected boolean verifyAccessToken(AccessTokenGitlabDto accessToken) {
        return accessToken != null && accessToken.getAccessToken() != null && !accessToken.getAccessToken().isEmpty();
    }
    
    
    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreController.getSCMOrgToken(getBaseUrl(), orgId);
        String path = String.format(URL_GET_REPOS, orgId);
        ResponseEntity<RepoGitlabDto[]> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                RepoGitlabDto[].class, scmAccessTokenDto.getAccessToken());
        ArrayList<RepoGitlabDto> repoGitlabDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        List<RepoGitlabDto> filteredRepos = repoGitlabDtos.stream().filter(repoGitlabDto -> repoGitlabDto.getNamespace().getId().equals(orgId)).collect(Collectors.toList());
        for (RepoGitlabDto repoGitlabDto : filteredRepos) {
            WebhookGitLabDto webhookGitlabDto = getRepositoryCxFlowWebhook(repoGitlabDto.getId(),
                    scmAccessTokenDto.getAccessToken());
            if(webhookGitlabDto != null) {
                repoGitlabDto.setWebHookEnabled(true);
                repoGitlabDto.setWebhookId(webhookGitlabDto.getId());
            } else {
                repoGitlabDto.setWebHookEnabled(false);
            }
        }
        OrgReposDto orgReposDto = Converter.convertToOrgGitlabRepoDto(scmAccessTokenDto, filteredRepos);
        dataStoreController.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoGitlabWebDto(filteredRepos);
    }


    @Override
    public String createWebhook(@NonNull String orgId, @NonNull String projectId ) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreController.getSCMOrgToken(getBaseUrl(), orgId);
        String path = String.format(URL_CREATE_WEBHOOK, projectId, scmAccessTokenDto.getAccessToken()) ;
         ResponseEntity<WebhookGitLabDto> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.POST,
                 new WebhookGitLabDto(), null,
                WebhookGithubDto.class,
                scmAccessTokenDto.getAccessToken());
        WebhookGitLabDto webhookGithubDto = response.getBody();
        if(webhookGithubDto == null || StringUtils.isEmpty(webhookGithubDto.getId())){
            log.error(RestHelper.WEBHOOK_CREATE_FAILURE);
            throw new ScmException(RestHelper.WEBHOOK_CREATE_FAILURE);
        }
        com.checkmarx.dto.datastore.RepoDto repoDto = com.checkmarx.dto.datastore.RepoDto.builder().repoId(projectId).isWebhookConfigured(true).webhookId(
                webhookGithubDto.getId()).build();
        dataStoreController.updateScmOrgRepo(OrgReposDto.builder()
                .orgName(scmAccessTokenDto.getOrgId())
                .scmUrl(scmAccessTokenDto.getScmUrl())
                .repoList(Collections.singletonList(repoDto))
                .build());
        return webhookGithubDto.getId();
    }

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String webhookId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreController.getSCMOrgToken(getBaseUrl(), orgId);
        String path = String.format(URL_DELETE_WEBHOOK, orgId, repoId, webhookId);

        try {
            restHelper.sendBearerAuthRequest(path, HttpMethod.DELETE,null, null,
                    WebhookGithubDto.class,
                    scmAccessTokenDto.getAccessToken());
        } catch (HttpClientErrorException ex){
            if(ex.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                log.error("Webhook not found: {}", ex.getMessage());
                throw new ScmException(RestHelper.WEBHOOK_DELETE_FAILURE);
            }
            throw new ScmException(RestHelper.GENERAL_RUNTIME_EXCEPTION);
        }
        com.checkmarx.dto.datastore.RepoDto repoDto =
                com.checkmarx.dto.datastore.RepoDto.builder().repoId(repoId).webhookId(null).isWebhookConfigured(false).build();
        dataStoreController.updateScmOrgRepo(OrgReposDto.builder()
                .orgName(scmAccessTokenDto.getOrgId())
                .scmUrl(scmAccessTokenDto.getScmUrl())
                .repoList(Collections.singletonList(repoDto))
                .build());
    }

    @Override
    public CxFlowConfigDto validateCxFlowConfiguration(@NonNull CxFlowConfigDto cxFlowConfigDto) {
        //TODO
        return null;
    }



    private WebhookGitLabDto getRepositoryCxFlowWebhook(@NonNull String repoId,
                                                        @NonNull String accessToken){
        String path = String.format(URL_GET_WEBHOOKS, repoId);
        ResponseEntity<WebhookGitLabDto[]> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                WebhookGitLabDto[].class, accessToken);
        ArrayList<WebhookGitLabDto> webhookGithubDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (WebhookGitLabDto webhookGitLabDto : webhookGithubDtos) {
            if (webhookGitLabDto != null  && webhookGitLabDto.getUrl().equals(cxFlowWebHook))
                return webhookGitLabDto;
        }
        return null;
    }
}
