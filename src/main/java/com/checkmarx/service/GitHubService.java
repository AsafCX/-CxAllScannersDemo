package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.github.*;
import com.checkmarx.dto.BaseDto;

import com.checkmarx.dto.IRepoDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.AccessTokenManager;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestWrapper;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service("github")
public class GitHubService extends AbstractScmService implements ScmService {
    
    private static final String URL_GENERATE_TOKEN = "https://github.com/login/oauth/access_token" +
            "?client_id=%s&client_secret=%s&code=%s";

    private static final String URL_GET_ORGANIZATIONS = "https://api.github.com/user/orgs";

    public static final String URL_GET_REPOS = "https://api.github" +
            ".com/orgs/%s/repos?type=all&per_page=100";
    
    private static final String URL_WEBHOOK_OPERATION = "https://api.github.com/repos/%s/%s/hooks";
    
    private static final String URL_DELETE_WEBHOOK = "https://api.github.com/repos/%s/%s/hooks/%s";

    private static final String URL_VALIDATE_TOKEN = "https://api.github.com/user";

    private static final String GIT_HUB_URL = "github.com";
    
    private static final String SCOPES = "repo,admin:repo_hook,read:org,read:user";

    private static final String INVALID_TOKEN = "Github token validation failure";


    @Override
    public List<OrganizationWebDto> getOrganizations(@NonNull String authCode) {
        AccessTokenManager accessTokenManager = generateAccessToken(authCode);
        log.info("Access token generated successfully");

        ResponseEntity<OrganizationGithubDto[]> response =
                restWrapper.sendBearerAuthRequest(URL_GET_ORGANIZATIONS, HttpMethod.GET, null, null,
                                                  OrganizationGithubDto[].class, accessTokenManager.getAccessTokenStr());
        List<OrganizationGithubDto> userOrgGithubDtos =
                new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
        List<OrgDto> orgDtos =
                Converter.convertToListOrg(accessTokenManager.getAccessTokenJson(), userOrgGithubDtos,
                                                      getBaseDbKey());
        dataStoreService.storeOrgs(orgDtos);
        return Converter.convertToListOrgWebDtos(userOrgGithubDtos);
    }

    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {

        AccessTokenManager accessTokenWrapper = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);

        String path = String.format(URL_GET_REPOS, orgId);
        ResponseEntity<RepoGithubDto[]> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
                                       null, null,
                                       RepoGithubDto[].class, accessTokenWrapper.getAccessTokenStr());
        ArrayList<RepoGithubDto> orgRepoGithubDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));

        List<IRepoDto> outputDTOs =  new ArrayList<>();
        for (IRepoDto repoDto : orgRepoGithubDtos) {
            try {
                WebhookGithubDto webhookDto = getRepositoryCxFlowWebhook(orgId, repoDto.getName(),
                        accessTokenWrapper.getAccessTokenStr());
                setWebhookDetails(repoDto, webhookDto);
                outputDTOs.add(repoDto);
            } catch (HttpClientErrorException ex){
                if(ex.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                    log.info("User can't access repository '{}' webhook settings",
                            repoDto.getName());
                }
            }
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(accessTokenWrapper.getDbDto(), outputDTOs);
        dataStoreService.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(outputDTOs);
    }
    
    @Override
    public BaseDto createWebhook(@NonNull String orgId, @NonNull String repoId) {
        AccessTokenManager accessTokenWrapper = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);

        String path = String.format(URL_WEBHOOK_OPERATION, orgId, repoId);
        WebhookGithubDto webhookGithubDto = initWebhook();
        ResponseEntity<WebhookGithubDto> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.POST,
                                       webhookGithubDto, null,
                                       WebhookGithubDto.class,
                                       accessTokenWrapper.getAccessTokenStr());
        webhookGithubDto = response.getBody();
        validateWebhookDto(webhookGithubDto);
        dataStoreService.updateWebhook(repoId, accessTokenWrapper.getDbDto(),webhookGithubDto.getId(), true);

        return new BaseDto(webhookGithubDto.getId());
    }

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String deleteUrl) {
        String path = String.format(URL_DELETE_WEBHOOK, orgId, repoId, deleteUrl);
        super.deleteWebhook(orgId, repoId, path, WebhookGithubDto.class);
    }

    @Override
    public CxFlowConfigDto getCxFlowConfiguration(@NonNull String orgId) {
        AccessTokenManager accessTokenWrapper = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);

        CxFlowConfigDto cxFlowConfigDto = getOrganizationSettings(orgId, accessTokenWrapper.getAccessTokenStr());
        validateCxFlowConfig(cxFlowConfigDto);
        return cxFlowConfigDto;
    }

    private void validateCxFlowConfig(CxFlowConfigDto cxFlowConfigDto) {
        if(StringUtils.isEmpty(cxFlowConfigDto.getScmAccessToken()) || StringUtils.isEmpty(
                cxFlowConfigDto.getTeam()) || StringUtils.isEmpty(cxFlowConfigDto.getCxgoSecret())) {
            log.error("CxFlow configuration settings validation failure, missing data");
            throw new ScmException("CxFlow configuration settings validation failure, missing data");
        }
        try {
            restWrapper.sendBearerAuthRequest(URL_VALIDATE_TOKEN, HttpMethod.GET, null, null,
                                              CxFlowConfigDto.class,
                                              cxFlowConfigDto.getScmAccessToken());
        } catch (HttpClientErrorException ex) {
            log.error("{}: {}", INVALID_TOKEN, ex.getMessage());
            throw new ScmException(INVALID_TOKEN);
        }
        log.info("Github token validation passed successfully!");
    }

    private WebhookGithubDto initWebhook() {
        return  WebhookGithubDto.builder()
                .name("web")
                .config(WebhookGithubDto.Config.builder().contentType("json").url(getCxFlowUrl()).insecureSsl("0").secret("1234").build())
                .events(GithubEvent.getAllEventsList())
                .active(true)
                .build();
    }

    private WebhookGithubDto getRepositoryCxFlowWebhook(@NonNull String orgName, @NonNull String repoName,
                                                        @NonNull String accessToken){
        String path = String.format(URL_WEBHOOK_OPERATION, orgName, repoName);
        ResponseEntity<WebhookGithubDto[]> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
                                       null, null,
                                       WebhookGithubDto[].class, accessToken);
        ArrayList<WebhookGithubDto> webhookDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));

        return (WebhookGithubDto)getActiveHook(webhookDtos);
    }

    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitHub api
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitHub
     */
    private AccessTokenManager generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        String path = buildPathAccessToken(oAuthCode, scmDto);
        AccessTokenGithubDto accessTokenDto =  sendAccessTokenRequest(path);
        AccessTokenManager mgr = new AccessTokenManager(accessTokenDto, AccessTokenGithubDto.class);
        return mgr;
    }

    private AccessTokenGithubDto sendAccessTokenRequest(String path) {
        ResponseEntity<AccessTokenGithubDto> response = restWrapper.sendRequest(path, HttpMethod.POST,
                                                                          null, null, AccessTokenGithubDto.class);
        AccessTokenGithubDto accessTokenDto = response.getBody();
        if(!verifyAccessToken(accessTokenDto)){
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return accessTokenDto;
    }

    private String buildPathAccessToken(String oAuthCode, ScmDto scmDto) {
        return String.format(URL_GENERATE_TOKEN, scmDto.getClientId(),
                             scmDto.getClientSecret(),
                             oAuthCode);
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
