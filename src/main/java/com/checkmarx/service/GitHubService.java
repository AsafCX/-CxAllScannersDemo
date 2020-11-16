package com.checkmarx.service;

import com.checkmarx.controller.DataController;
import com.checkmarx.controller.exception.GitHubException;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.github.AccessTokenGithubDto;
import com.checkmarx.dto.github.OrgGithubDto;
import com.checkmarx.dto.github.RepoGithubDto;
import com.checkmarx.dto.github.WebhookGithubDto;
import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.checkmarx.dto.web.OrgWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.dto.web.ScmConfigWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestHelper;
import com.checkmarx.utils.TokenType;
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


@Slf4j
@Service("github")
public class GitHubService implements ScmService {

    @Value("${github.url.pattern.generate.oauth.token}")
    private String urlPatternGenerateOAuthToken;

    @Value("${github.url.pattern.get.user.organizations}")
    private String urlPatternGetUserOrganizations;

    @Value("${github.url.pattern.get.user.repositories}")
    private String urlPatternGetUserRepositories;

    @Value("${github.url.pattern.get.org.repositories}")
    private String urlPatternGetOrgRepositories;

    @Value("${github.url.pattern.repo.webhook}")
    private String urlPatternRepoWebhook;

    @Value("${github.url.pattern.repo.delete.webhook}")
    private String urlPatternRepoDeleteWebhook;

    @Value("${cxflow.webhook.url}")
    private String cxFlowWebHook;

    @Value("${github.url}")
    private String githubUrl;

    @Value("${github.scope}")
    private String scope;


    @Autowired
    DataController dataStoreController;

    @Autowired
    RestHelper restHelper;

    private Map<String, AccessTokenGithubDto> synchronizedMap =
            Collections.synchronizedMap(new HashMap<>());

    @Override
    public ScmConfigWebDto getScmConfiguration() {
        ScmDto scmDto = dataStoreController.getScm(githubUrl);
        return ScmConfigWebDto.builder().clientId(scmDto.getClientId()).scope(scope).build();
    }

    @Override
    public List<OrgWebDto> getOrganizations(@NonNull String authCode) {
        AccessTokenGithubDto accessToken = generateAccessToken(authCode);
        log.info("Access token generated successfully");

        ResponseEntity<OrgGithubDto[]> response =
                restHelper.sendBearerAuthRequest(urlPatternGetUserOrganizations, HttpMethod.GET, null, null,
                                                 OrgGithubDto[].class, accessToken.getAccessToken());
        ArrayList<OrgGithubDto> userOrgGithubDtos = new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
        addAccessToken(accessToken, userOrgGithubDtos);
        return Converter.convertToListOrgWebDtos(userOrgGithubDtos);
    }

    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgName) {
        AccessTokenGithubDto accessTokenGithubDto = getAccessToken(orgName);
        if (!verifyAccessToken(accessTokenGithubDto)) {
            log.error(RestHelper.ACCESS_TOKEN_MISSING + " orgName: {}", orgName);
            throw new GitHubException(RestHelper.ACCESS_TOKEN_MISSING);
        }
        ScmAccessTokenDto scmAccessTokenDto =
                ScmAccessTokenDto.builder()
                        .scmUrl(githubUrl)
                        .orgName(orgName)
                        .accessToken(accessTokenGithubDto.getAccessToken())
                        .tokenType(TokenType.ACCESS.getType())
                        .build();
        dataStoreController.storeScmOrgToken(scmAccessTokenDto);

        String path = String.format(urlPatternGetOrgRepositories, orgName);
        ResponseEntity<RepoGithubDto[]> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.GET,
                                                                                     null, null,
                                                                                     RepoGithubDto[].class, scmAccessTokenDto.getAccessToken());
        ArrayList<RepoGithubDto> orgRepoGithubDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (RepoGithubDto repoGithubDto : orgRepoGithubDtos) {
            WebhookGithubDto webhookGithubDto = getRepositoryCxFlowWebhook(orgName, repoGithubDto.getName(),
                                                                           accessTokenGithubDto.getAccessToken());
            if(webhookGithubDto != null) {
                repoGithubDto.setWebHookEnabled(true);
                repoGithubDto.setWebhookId(webhookGithubDto.getId());
            } else {
                repoGithubDto.setWebHookEnabled(false);
            }
        }
        ScmRepoDto scmRepoDto = Converter.convertToSCMRepoDto(scmAccessTokenDto, orgRepoGithubDtos);
        dataStoreController.updateScmOrgRepo(scmRepoDto);
        return Converter.convertToListRepoWebDto(orgRepoGithubDtos);
    }

    @Override
    public RepoDto getScmOrgRepo(@NonNull String orgName, @NonNull String repoName) {
       return dataStoreController.getScmOrgRepo(githubUrl, orgName, repoName);
    }

    @Override
    public String createWebhook(@NonNull String orgName, @NonNull String repoName) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreController.getSCMOrgToken(githubUrl, orgName);
        String path = String.format(urlPatternRepoWebhook, orgName, repoName);
        WebhookGithubDto webhookGithubDto = initWebhook();
        ResponseEntity<WebhookGithubDto> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.POST,
                                                                                      webhookGithubDto, null,
                                                                                      WebhookGithubDto.class,
                                                                                      scmAccessTokenDto.getAccessToken());
        webhookGithubDto = response.getBody();
        if(webhookGithubDto == null || StringUtils.isEmpty(webhookGithubDto.getId())){
            log.error(RestHelper.WEBHOOK_CREATE_FAILURE);
            throw new GitHubException(RestHelper.WEBHOOK_CREATE_FAILURE);
        }
        RepoDto repoDto = RepoDto.builder().name(repoName).isWebhookConfigured(true).webhookId(
                webhookGithubDto.getId()).build();
        dataStoreController.updateScmOrgRepo(ScmRepoDto.builder()
                                                     .orgName(scmAccessTokenDto.getOrgName())
                                                     .scmUrl(scmAccessTokenDto.getScmUrl())
                                                     .repoList(Collections.singletonList(repoDto))
                                                     .build());
        return webhookGithubDto.getId();
    }

    @Override
    public void deleteWebhook(@NonNull String orgName, @NonNull String repoName,
                              @NonNull String webhookId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreController.getSCMOrgToken(githubUrl, orgName);
        String path = String.format(urlPatternRepoDeleteWebhook, orgName, repoName, webhookId);

        try {
            restHelper.sendBearerAuthRequest(path, HttpMethod.DELETE,null, null,
                                             WebhookGithubDto.class,
                                             scmAccessTokenDto.getAccessToken());
        } catch (HttpClientErrorException ex){
            if(ex.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                log.error("Webhook not found: {}", ex.getMessage());
                throw new GitHubException(RestHelper.WEBHOOK_DELETE_FAILURE);
            }
            throw new GitHubException(RestHelper.GENERAL_RUNTIME_EXCEPTION);
        }
        RepoDto repoDto =
                RepoDto.builder().name(repoName).webhookId(null).isWebhookConfigured(false).build();
        dataStoreController.updateScmOrgRepo(ScmRepoDto.builder()
                                                     .orgName(scmAccessTokenDto.getOrgName())
                                                     .scmUrl(scmAccessTokenDto.getScmUrl())
                                                     .repoList(Collections.singletonList(repoDto))
                                                     .build());
    }

    @Override
    public OrgSettingsWebDto getCxGoSettings(@NonNull String orgName) {
        return dataStoreController.getScmOrgCxGo(githubUrl, orgName);
    }

    @Override
    public void setCxGoSettings(@NonNull String orgName, @NonNull OrgSettingsWebDto orgSettingsWebDto) {
        CxFlowPropertiesDto cxFlowPropertiesDto = Converter.convertToCxFlowProperties(githubUrl,
                                                                                      orgName,
                                                                                      orgSettingsWebDto);
        dataStoreController.setScmOrgCxGo(cxFlowPropertiesDto);
    }

    private WebhookGithubDto initWebhook() {
        return  WebhookGithubDto.builder()
                .name("web")
                .config(WebhookGithubDto.Config.builder().contentType("json").url(cxFlowWebHook).insecureSsl("0").secret("1234").build())
                .events(Arrays.asList("push", "pull_request"))
                .build();
    }

    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitHub api
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitHub
     */
    private AccessTokenGithubDto generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreController.getScm(githubUrl);
        String path = String.format(urlPatternGenerateOAuthToken, scmDto.getClientId(),
                                    scmDto.getClientSecret(),
                                    oAuthCode);
        ResponseEntity<AccessTokenGithubDto> response =  restHelper.sendRequest(path, HttpMethod.POST,
                                                                                null, null,
                                                                                AccessTokenGithubDto.class);
        if(!verifyAccessToken(response.getBody())){
            log.error(RestHelper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new GitHubException(RestHelper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return response.getBody();
    }

    private void addAccessToken(AccessTokenGithubDto accesToken, List<OrgGithubDto> orgsDto) {
        synchronized (synchronizedMap) {
            for (OrgGithubDto orgDto : orgsDto) {
                if (synchronizedMap.containsKey(orgDto.getLogin())) {
                    synchronizedMap.remove(orgDto.getLogin());
                }
                synchronizedMap.put(orgDto.getLogin(), accesToken);
            }
        }
    }

    private AccessTokenGithubDto getAccessToken(String orgName) {
        AccessTokenGithubDto accessToken = new AccessTokenGithubDto();
        synchronized (synchronizedMap) {
            if (synchronizedMap.containsKey(orgName)) {
                accessToken = synchronizedMap.get(orgName);
            }
        }
        return accessToken;
    }

    /**
     * verifyAccessToken method used to verify access token creation, Currently checks if access
     * token created(not null or empty) without GitHub validation
     *
     * @param accessToken access token generated before using GitHub api, Gives access to relevant
     *                  GitHub data
     * @return true if verification passed successfully
     */
    private boolean verifyAccessToken(AccessTokenGithubDto accessToken) {
        return accessToken != null && accessToken.getAccessToken() != null && !accessToken.getAccessToken().isEmpty();
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
