package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.IDto;
import com.checkmarx.dto.IRepoDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.github.OrganizationGithubDto;
import com.checkmarx.dto.github.RepoGithubDto;
import com.checkmarx.dto.github.WebhookGithubDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;


@Slf4j
@Service("github")
public class GitHubService extends AbstractScmService  implements ScmService {
    
    private String URL_GENERATE_TOKEN = "https://github.com/login/oauth/access_token?client_id=%s&client_secret=%s&code=%s";
    
    private String URL_GET_ORGANIZATIONS = "https://api.github.com/user/orgs";
    
    private String URL_GET_REPOS = "https://api.github.com/orgs/%s/repos?type=all&per_page=100";
    
    private String URL_GET_WEBHOOK = "https://api.github.com/repos/%s/%s/hooks";
    
    private String URL_DELETE_WEBHOOK = "https://api.github.com/repos/%s/%s/hooks/%s";

    private String URL_VALIDATE_TOKEN = "https://api.github.com/user";

    private String GIT_HUB_URL = "github.com";
    
    private String SCOPES = "repo,admin:repo_hook,read:org,read:user";


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
        ResponseEntity<AccessTokenDto> response = generateAccessToken(restWrapper, path, null, null);
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
    

    protected String buildPathAccessToken(String oAuthCode, ScmDto scmDto) {
        return String.format(URL_GENERATE_TOKEN, scmDto.getClientId(),
                scmDto.getClientSecret(),
                oAuthCode);
    }
    

    @Override
    public List<OrganizationWebDto> getOrganizations(@NonNull String authCode) {
        AccessTokenDto accessToken = generateAccessToken(authCode);
        log.info("Access token generated successfully");

        ResponseEntity<? extends IDto[]> response =
                restWrapper.sendBearerAuthRequest(URL_GET_ORGANIZATIONS, HttpMethod.GET, null, null,
                                                  OrganizationGithubDto[].class, accessToken.getAccessToken());
        List<? extends IDto> userOrgGithubDtos = new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
        List<ScmAccessTokenDto> scmAccessTokenDtos =
                Converter.convertToListOrgAccessToken(accessToken, userOrgGithubDtos, getBaseDbKey());
        dataStoreService.storeScmOrgsToken(scmAccessTokenDtos);
        return Converter.convertToListOrgWebDtos(userOrgGithubDtos);
    }

    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {

        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        String path = String.format(URL_GET_REPOS, orgId);
        ResponseEntity<RepoGithubDto[]> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
                                       null, null,
                                       RepoGithubDto[].class, scmAccessTokenDto.getAccessToken());
        ArrayList<RepoGithubDto> orgRepoGithubDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));

        List<IRepoDto> outputDTOs =  new ArrayList<>();
        for (IRepoDto repoDto : orgRepoGithubDtos) {
            try {
                WebhookGithubDto webhookGithubDto = getRepositoryCxFlowWebhook(orgId, repoDto.getName(),
                        scmAccessTokenDto.getAccessToken());
                if (webhookGithubDto != null) {
                    repoDto.setWebHookEnabled(true);
                    repoDto.setWebhookId(webhookGithubDto.getId());
                } else {
                    repoDto.setWebHookEnabled(false);
                }
                outputDTOs.add(repoDto);
            } catch (HttpClientErrorException ex){
                if(ex.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                    log.info("User can't access repository '{}' webhook settings",
                            repoDto.getName());
                }
            }
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(scmAccessTokenDto, outputDTOs);
        dataStoreService.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(outputDTOs);
    }
    
    @Override
    public String createWebhook(@NonNull String orgId, @NonNull String repoId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(GIT_HUB_URL, orgId);
        String path = String.format(URL_GET_WEBHOOK, orgId, repoId);
        WebhookGithubDto webhookGithubDto = initWebhook();
        ResponseEntity<WebhookGithubDto> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.POST,
                                       webhookGithubDto, null,
                                       WebhookGithubDto.class,
                                       scmAccessTokenDto.getAccessToken());
        webhookGithubDto = response.getBody();
        if(webhookGithubDto == null || StringUtils.isEmpty(webhookGithubDto.getId())){
            log.error(RestWrapper.WEBHOOK_CREATE_FAILURE);
            throw new ScmException(RestWrapper.WEBHOOK_CREATE_FAILURE);
        }

        dataStoreService.updateWebhook(repoId, scmAccessTokenDto,webhookGithubDto.getId(), true);

        return webhookGithubDto.getId();
    }
    

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String webhookId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(GIT_HUB_URL, orgId);
        String path = String.format(URL_DELETE_WEBHOOK, orgId, repoId, webhookId);

        try {
            restWrapper.sendBearerAuthRequest(path, HttpMethod.DELETE, null, null,
                                              WebhookGithubDto.class,
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
        if(StringUtils.isEmpty(cxFlowConfigDto.getScmAccessToken()) || StringUtils.isEmpty(cxFlowConfigDto.getTeam()) || StringUtils.isEmpty(cxFlowConfigDto.getCxgoSecret())) {
            log.error("CxFlow configuration settings validation failure, missing data");
            throw new ScmException("CxFlow configuration settings validation failure, missing data");
        }
        try {
            restWrapper.sendBearerAuthRequest(URL_VALIDATE_TOKEN, HttpMethod.GET, null, null,
                                              CxFlowConfigDto.class,
                                              cxFlowConfigDto.getScmAccessToken());
        } catch (HttpClientErrorException ex){
            log.error("Github Token validation failure: {}", ex.getMessage());
            throw new ScmException("Github Token authorization failure");
        }
        log.info("Github token validation passed successfully!");
        return cxFlowConfigDto;
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
        String path = String.format(URL_GET_WEBHOOK, orgName, repoName);
        ResponseEntity<WebhookGithubDto[]> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
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

    @Override
    public String getScopes() {
        return SCOPES;
    }

    @Override
    public String getBaseDbKey() {
        return GIT_HUB_URL;
    }


}
