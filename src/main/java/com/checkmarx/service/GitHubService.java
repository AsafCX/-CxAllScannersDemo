package com.checkmarx.service;

import com.checkmarx.controller.DataController;
import com.checkmarx.controller.exception.GitHubException;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.datastore.ScmRepoDto;
import com.checkmarx.dto.github.AccessTokenGithubDto;
import com.checkmarx.dto.github.OrgGithubDto;
import com.checkmarx.dto.github.RepoGithubDto;
import com.checkmarx.dto.github.WebhookGithubDto;
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
    public ScmDto getScm(@NonNull String baseUrl) {
        return dataStoreController.getScm(baseUrl);
    }

    @Override
    public @NonNull void storeScmOrgToken(@NonNull ScmAccessTokenDto scmAccessTokenDto) {
        dataStoreController.storeScmOrgToken(scmAccessTokenDto);
    }

    @Override
    public ScmAccessTokenDto getScmOrgToken(@NonNull String scmUrl, @NonNull String orgName) {
        return dataStoreController.getSCMOrgToken(scmUrl, orgName);
    }

    @Override
    public List<OrgWebDto> getOrganizations(@NonNull String authCode) {
        AccessTokenGithubDto accessToken = generateAccessToken(authCode);
        log.info("Access token generated successfully");
        ArrayList<OrgGithubDto> userOrgGithubDtos = getUserOrganizations(accessToken.getAccessToken());
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
        storeScmOrgToken(scmAccessTokenDto);

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
        updateScmOrgRepos(scmRepoDto);

        return Converter.convertToListRepoWebDto(orgRepoGithubDtos);
    }

    @Override
    public RepoDto getScmOrgRepo(@NonNull String githubUrl, @NonNull String orgName,
                                 @NonNull String repoName) {
       return dataStoreController.getScmOrgRepo(githubUrl, orgName, repoName);
    }

    @Override
    public void storeScmOrgRepos(@NonNull ScmRepoDto scmRepoDto) {
        dataStoreController.storeScmOrgRepos(scmRepoDto);
    }

    @Override
    public void updateScmOrgRepos(@NonNull ScmRepoDto scmRepoDto) {
        dataStoreController.updateScmOrgRepo(scmRepoDto);
    }

    @Override
    public String createWebhook(@NonNull String orgName, @NonNull String repoName) {
        ScmAccessTokenDto scmAccessTokenDto = getScmOrgToken(githubUrl, orgName);
        String path = String.format(urlPatternRepoWebhook, orgName, repoName);
        WebhookGithubDto webhookGithubDto = initWebhook();
        ResponseEntity<WebhookGithubDto> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.POST,
                                                                                      webhookGithubDto, null,
                                                                                      WebhookGithubDto.class,
                                                                                      scmAccessTokenDto.getAccessToken());
        webhookGithubDto = response.getBody();
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
        ScmAccessTokenDto scmAccessTokenDto = getScmOrgToken(githubUrl, orgName);
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

    public WebhookGithubDto initWebhook() {
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
        ScmDto scmDto = getScm(githubUrl);
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

    /**
     * getUserOrganizations method using access token retrieve all user organisations via GitHub api
     *
     * @param accessToken generated before using GitHub api, Gives access to relevant GitHub data
     * @return Array list of all user organizations
     */
    private ArrayList<OrgGithubDto> getUserOrganizations(@NonNull String accessToken) {

        ResponseEntity<OrgGithubDto[]> response =
                restHelper.sendBearerAuthRequest(urlPatternGetUserOrganizations, HttpMethod.GET, null, null,
                                                 OrgGithubDto[].class, accessToken);
        return new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
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
