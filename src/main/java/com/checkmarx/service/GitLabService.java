package com.checkmarx.service;

import com.checkmarx.controller.DataController;
import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.github.AccessTokenGithubDto;
import com.checkmarx.dto.github.OrganizationGithubDto;
import com.checkmarx.dto.github.RepoGithubDto;
import com.checkmarx.dto.github.WebhookGithubDto;
import com.checkmarx.dto.gitlab.AccessTokenGitlabDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestHelper;
import com.checkmarx.utils.TokenType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service("gitlab")
public class GitLabService implements ScmService  {
    
    private String URL_AUTH_TOKEN = "https://gitlab.com/oauth/token?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s";

    private String BASE_URL = "https://gitlab.com/api/v4";

    private String URL_GET_NAMESPACES = BASE_URL + "/namespaces/";

    private String URL_GET_REPOS = BASE_URL + "/projects?simple=true/";

    private String GITLAB_BASE_URL = "gitlab.com";

    private String SCOPES ="api";
    
    @Value("${redirect.url}")
    private String redirectUrl;

    @Autowired
    RestHelper restHelper;

    @Autowired
    DataController dataStoreController;
    
    private String urlPatternRepoWebhook;
    
    private String urlPatternRepoDeleteWebhook;

    @Value("${cxflow.webhook.url}")
    private String cxFlowWebHook;
    
    
    @Override
    public String getScopes() {
        return SCOPES;
    }
    

    public String getBaseUrl() {
        return GITLAB_BASE_URL;
    }

    protected String buildTokenPath(String oAuthCode, ScmDto scmDto) {
        return String.format(URL_AUTH_TOKEN, scmDto.getClientId(),
                scmDto.getClientSecret(),
                oAuthCode,
                redirectUrl);
    }
    
//    @Override
////    public List<OrganizationWebDto> getOrganizations(@NonNull String authCode) {
////        AccessTokenGitlabDto accessToken =  generateAccessToken(authCode, AccessTokenGitlabDto.class);
////        log.info("Access token generated successfully");
////
////        ResponseEntity<OrganizationWebDto[]> response =
////                restHelper.sendBearerAuthRequest(URL_GET_NAMESPACES, HttpMethod.GET, null, null,
////                        OrganizationWebDto[].class, accessToken.getAccessToken());
////        ArrayList<OrganizationWebDto> userOrgGithubDtos = new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
////        saveAccessToken(accessToken, userOrgGithubDtos);
////        return userOrgGithubDtos;
////    }

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
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgName) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreController.getSCMOrgToken(getBaseUrl(), orgName);
        String path = String.format(URL_GET_REPOS, orgName);
        ResponseEntity<RepoGithubDto[]> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                RepoGithubDto[].class, scmAccessTokenDto.getAccessToken());
        ArrayList<RepoGithubDto> orgRepoGithubDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (RepoGithubDto repoGithubDto : orgRepoGithubDtos) {
            WebhookGithubDto webhookGithubDto = getRepositoryCxFlowWebhook(orgName, repoGithubDto.getName(),
                    scmAccessTokenDto.getAccessToken());
            if(webhookGithubDto != null) {
                repoGithubDto.setWebHookEnabled(true);
                repoGithubDto.setWebhookId(webhookGithubDto.getId());
            } else {
                repoGithubDto.setWebHookEnabled(false);
            }
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(scmAccessTokenDto, orgRepoGithubDtos);
        dataStoreController.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(orgRepoGithubDtos);
    }
    

    @Override
    public String createWebhook(@NonNull String orgName, @NonNull String repoName) {
         //TODO
        return "";
    }

    @Override
    public void deleteWebhook(@NonNull String orgName, @NonNull String repoName,
                              @NonNull String webhookId) {
        //TODO
    }



    private WebhookGithubDto initWebhook() {
        return  WebhookGithubDto.builder()
                .name("web")
                .config(WebhookGithubDto.Config.builder().contentType("json").url(cxFlowWebHook).insecureSsl("0").secret("1234").build())
                .events(Arrays.asList("push", "pull_request"))
                .build();
    }
    

      private WebhookGithubDto getRepositoryCxFlowWebhook(@NonNull String orgName, @NonNull String repoName,
                                                        @NonNull String accessToken){
        String path = String.format(urlPatternRepoWebhook, orgName, repoName);
        ResponseEntity<WebhookGithubDto[]> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                WebhookGithubDto[].class, accessToken);
        ArrayList<WebhookGithubDto> webhookGithubDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (WebhookGithubDto webHookGithubDto : webhookGithubDtos) {
            if (webHookGithubDto != null && webHookGithubDto.getActive() && webHookGithubDto.getConfig().getUrl().equals(cxFlowWebHook))
                return webHookGithubDto;
        }
        return null;
    }
}
