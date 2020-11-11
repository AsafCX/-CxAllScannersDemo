package com.checkmarx.service;

import com.checkmarx.controller.DataStoreController;
import com.checkmarx.controller.exception.GitHubException;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.SCMAccessTokenDto;
import com.checkmarx.dto.datastore.SCMDto;
import com.checkmarx.dto.datastore.SCMRepoDto;
import com.checkmarx.dto.github.AccessTokenDto;
import com.checkmarx.dto.github.OrganizationDto;
import com.checkmarx.dto.github.RepositoryDto;
import com.checkmarx.dto.github.WebhookDto;
import com.checkmarx.dto.web.ScmConfigDto;
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
    DataStoreController dataStoreController;

    @Autowired
    RestHelper restHelper;

    private Map<String, AccessTokenDto> synchronizedMap =
            Collections.synchronizedMap(new HashMap<String, AccessTokenDto>());

    public void storeScm(SCMDto scmDto) {
        dataStoreController.storeScm(scmDto);
    }

    public void storeSCMOrgToken(SCMAccessTokenDto scmAccessTokenDto) {
        dataStoreController.storeSCMOrgToken(scmAccessTokenDto);
    }

    public SCMAccessTokenDto getSCMOrgToken(String scmUrl,String orgName) {
        return dataStoreController.getSCMOrgToken(scmUrl, orgName);
    }

    @Override
    public @NonNull List<OrganizationDto> getOrganizations(String oAuthCode) {
        AccessTokenDto accessToken = generateAccessToken(oAuthCode);
        log.info("Access token generated successfully");
        ArrayList<OrganizationDto> userOrganizationDtos = getUserOrganizations(accessToken.getAccessToken());
        addAccessToken(accessToken, userOrganizationDtos);
        return userOrganizationDtos;
    }

    public SCMDto getScm(String baseUrl) {
        return dataStoreController.getScm(baseUrl);
    }

    public void storeSCMOrgRepos(SCMAccessTokenDto scmAccessTokenDto, List<RepositoryDto> orgRepositoryDtos) {
        SCMRepoDto scmRepoDto = Converter.convertToSCMRepoDto(scmAccessTokenDto, orgRepositoryDtos);
        dataStoreController.storeSCMOrgRepos(scmRepoDto);
    }

    public List<RepositoryDto> getSCMOrgRepos(String orgName) {
        AccessTokenDto accessTokenDto = getAccessToken(orgName);
        if (!verifyAccessToken(accessTokenDto)) {
            log.error(RestHelper.ACCESS_TOKEN_MISSING + " orgName: {}", orgName);
            throw new GitHubException(RestHelper.ACCESS_TOKEN_MISSING);
        }
        SCMAccessTokenDto scmAccessTokenDto =
                SCMAccessTokenDto.builder()
                        .scmUrl(githubUrl)
                        .orgName(orgName)
                        .accessToken(accessTokenDto.getAccessToken())
                        .tokenType(TokenType.ACCESS.getType())
                        .build();
        storeSCMOrgToken(scmAccessTokenDto);

        String path = String.format(urlPatternGetOrgRepositories, orgName);
        ResponseEntity<RepositoryDto[]> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.GET,
                                                                                     null, null,
                                                                                     RepositoryDto[].class, scmAccessTokenDto.getAccessToken());
        ArrayList<RepositoryDto> orgRepositoryDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (RepositoryDto repositoryDto : orgRepositoryDtos) {
            WebhookDto webhookDto = getRepositoryCXFlowWebHook(orgName, repositoryDto.getName(),
                                                               accessTokenDto.getAccessToken());
            if(webhookDto != null) {
                repositoryDto.setWebHookEnabled(true);
                repositoryDto.setWebhookId(webhookDto.getId());
            } else {
                repositoryDto.setWebHookEnabled(false);
            }
        }
        updateSCMOrgRepos(scmAccessTokenDto, orgRepositoryDtos);
        return orgRepositoryDtos;
    }

    public RepoDto getSCMOrgRepo(String githubUrl, String orgName, String repoName) {
       return dataStoreController.getSCMOrgRepo(githubUrl, orgName, repoName);
    }

    public void updateSCMOrgRepos(SCMAccessTokenDto scmAccessTokenDto, List<RepositoryDto> orgRepositoryDtos) {
        SCMRepoDto scmRepoDto = Converter.convertToSCMRepoDto(scmAccessTokenDto, orgRepositoryDtos);
        dataStoreController.updateSCMOrgRepo(scmRepoDto);
    }

    public void updateSCMOrgRepoWebhook(SCMAccessTokenDto scmAccessTokenDto, RepoDto repoDto) {
        dataStoreController.updateSCMOrgRepo(SCMRepoDto.builder()
                                                     .orgName(scmAccessTokenDto.getOrgName())
                                                     .scmUrl(scmAccessTokenDto.getScmUrl())
                                                     .repoList(Collections.singletonList(repoDto))
                                                     .build());
    }

    @Override
    public @NonNull void createWebhook(String orgName, String repoName) {
        SCMAccessTokenDto scmAccessTokenDto = getSCMOrgToken(githubUrl, orgName);
        String path = String.format(urlPatternRepoWebhook, orgName, repoName);
        WebhookDto webhookDto = initWebhook();
        ResponseEntity<WebhookDto> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.POST,
                                                                                webhookDto, null,
                                                                                WebhookDto.class,
                                                                                scmAccessTokenDto.getAccessToken());
        webhookDto = response.getBody();
        RepoDto repoDto = RepoDto.builder().name(repoName).isWebhookConfigured(true).webhookId(webhookDto.getId()).build();
        updateSCMOrgRepoWebhook(scmAccessTokenDto, repoDto);
    }

    @Override
    public @NonNull void deleteWebhook(String orgName, String repoName) {
        SCMAccessTokenDto scmAccessTokenDto = getSCMOrgToken(githubUrl, orgName);
        RepoDto repoDto = getSCMOrgRepo(githubUrl, orgName, repoName);
        String path = String.format(urlPatternRepoDeleteWebhook, orgName, repoName, repoDto.getWebhookId());

        try {
            restHelper.sendBearerAuthRequest(path, HttpMethod.DELETE,null, null,
                                             WebhookDto.class,
                                             scmAccessTokenDto.getAccessToken());
        } catch (HttpClientErrorException ex){
            if(ex.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                log.error("Webhook not found: {}", ex.getMessage());
                throw new GitHubException(RestHelper.WEBHOOK_DELETE_FAILURE);
            }
            throw new GitHubException(RestHelper.GENERAL_RUNTIME_EXCEPTION);
        }
        repoDto.setWebhookConfigured(false);
        repoDto.setWebhookId(null);
        updateSCMOrgRepoWebhook(scmAccessTokenDto, repoDto);
    }

    @Override
    public @NonNull List<RepositoryDto> getUserRepositories(String userAccessToken) {
        ResponseEntity<RepositoryDto[]> response =
                restHelper.sendBearerAuthRequest(urlPatternGetUserRepositories,
                                                 HttpMethod.GET, null, null,
                                                 RepositoryDto[].class, userAccessToken);
        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    @Override
    public @NonNull ScmConfigDto getScmConfiguration() {
        SCMDto scmDto = dataStoreController.getScm(githubUrl);
        return ScmConfigDto.builder().clientId(scmDto.getClientId()).scope(scope).build();
    }

    public WebhookDto initWebhook() {
        return  WebhookDto.builder()
                .name("web")
                .config(WebhookDto.Config.builder().contentType("json").url(cxFlowWebHook).insecureSsl("0").secret("1234").build())
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
    private AccessTokenDto generateAccessToken(String oAuthCode) {
        SCMDto scmDto = getScm(githubUrl);
        if (scmDto == null || StringUtils
                .isEmpty(scmDto.getClientId()) || StringUtils.isEmpty(scmDto.getClientSecret())){
            log.error(RestHelper.SCM_DETAILS_MISSING);
            throw new GitHubException(RestHelper.SCM_DETAILS_MISSING);
        }
        String path = String.format(urlPatternGenerateOAuthToken, scmDto.getClientId(),
                                    scmDto.getClientSecret(),
                                    oAuthCode);
        ResponseEntity<AccessTokenDto> response =  restHelper.sendRequest(path, HttpMethod.POST,
                                                                          null, null,
                                                                          AccessTokenDto.class);
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
    private ArrayList<OrganizationDto> getUserOrganizations(String accessToken) {

        ResponseEntity<OrganizationDto[]> response =
                restHelper.sendBearerAuthRequest(urlPatternGetUserOrganizations, HttpMethod.GET, null, null,
                                                 OrganizationDto[].class, accessToken);
        return new ArrayList<>(Arrays.asList(Objects.requireNonNull(response.getBody())));
    }

    public void addAccessToken(AccessTokenDto accesToken, List<OrganizationDto> orgsDto) {
        synchronized (synchronizedMap) {
            for (OrganizationDto orgDto : orgsDto) {
                if (synchronizedMap.containsKey(orgDto.getLogin())) {
                    synchronizedMap.remove(orgDto.getLogin());
                }
                synchronizedMap.put(orgDto.getLogin(), accesToken);
            }
        }
    }

    public AccessTokenDto getAccessToken(String orgName) {
        AccessTokenDto accessToken = new AccessTokenDto();
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
    private boolean verifyAccessToken(AccessTokenDto accessToken) {
        return accessToken != null && accessToken.getAccessToken() != null && !accessToken.getAccessToken().isEmpty();
    }

    private WebhookDto getRepositoryCXFlowWebHook(String orgName, String repoName,
                                                  String accessToken){
        String path = String.format(urlPatternRepoWebhook, orgName, repoName);
        ResponseEntity<WebhookDto[]> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.GET,
                                                                                  null, null,
                                                                                  WebhookDto[].class, accessToken);
        ArrayList<WebhookDto> webhookDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (WebhookDto webHookDto : webhookDtos) {
            if (webHookDto != null && webHookDto.getActive() && webHookDto.getConfig().getUrl().equals(cxFlowWebHook))
                return webHookDto;
        }
        return null;
    }
}
