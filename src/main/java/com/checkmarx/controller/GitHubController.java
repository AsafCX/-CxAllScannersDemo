package com.checkmarx.controller;

import com.checkmarx.controller.exception.GitHubException;
import com.checkmarx.dto.SCMAccessTokenDto;
import com.checkmarx.dto.SCMDto;
import com.checkmarx.dto.github.WebhookDto;
import com.checkmarx.dto.github.GitHubConfigDto;
import com.checkmarx.dto.github.AccessTokenDto;
import com.checkmarx.dto.github.OrganizationDto;
import com.checkmarx.dto.github.RepositoryDto;
import com.checkmarx.service.GitHubService;
import com.checkmarx.utils.RestHelper;
import com.checkmarx.utils.TokenType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping(value = "/github")
public class GitHubController {

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

    @Value("${cxflow.webhook.url}")
    private String cxFlowWebHook;

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.scope}")
    private String scope;

    @Value("${github.url}")
    private String githubUrl;

    @Autowired
    GitHubService gitHubService;

    @Autowired
    RestHelper restHelper;

    /**
     * Rest api used by FE application on start-up, Retrieve client id from env variable and
     * scope from app properties file
     *
     * @return ResponseEntity with status:200, Body: Github client id & scope
     */
    @GetMapping(value = "/config")
    public ResponseEntity getGitHubConfig() {

        log.trace("getGitHubConfig");
        //TODO Should be remove from env variables
        GitHubConfigDto config = new GitHubConfigDto(clientId, scope);
        SCMDto scmDto = new SCMDto(githubUrl, clientId, clientSecret);
        gitHubService.storeScm(scmDto);

        return ResponseEntity.ok(config);
    }

    /**
     * Rest api used to first create OAuth access token and retrieve all user organizations from GitHub
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return ResponseEntity with status:200, Body: list of all user organizations
     */
    @PostMapping(value = "/user/orgs")
    public ResponseEntity getOrganizations(@RequestParam(name = "code") String oAuthCode) {

        log.trace("getOrganizations: code={}", oAuthCode);
        AccessTokenDto accessToken = generateAccessToken(oAuthCode);
        log.info("Access token generated successfully");

        ArrayList<OrganizationDto> userOrganizationDtos = getUserOrganizations(accessToken.getAccessToken());
        gitHubService.addAccessToken(accessToken, userOrganizationDtos);
        return ResponseEntity.ok(userOrganizationDtos);
    }

    /**
     * Rest api used to get all user repositories from GitHub
     *
     * @param userAccessToken access token using it for GitHub authorization header
     * @return ResponseEntity with https status:200, Body: list of all user repositories (private
     *         and public)
     */
    @GetMapping(value = "/user/repos")
    public ResponseEntity getUserRepositories(
            @RequestHeader("UserAccessToken") String userAccessToken) {
        log.trace("getUserRepositories: UserAccessToken={}", userAccessToken);

        ResponseEntity<RepositoryDto[]> response =
                restHelper.sendBearerAuthRequest(urlPatternGetUserRepositories,
                                                                HttpMethod.GET, null, null,
                                       RepositoryDto[].class, userAccessToken);
        ArrayList<RepositoryDto> userRepositoryDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        return ResponseEntity.ok(userRepositoryDtos);
    }

    /**
     * Rest api used to get for specific organization all repositories (private and public)
     *
     * @param orgName organization name used to retrieve the relevant repositories
     * @return ResponseEntity with http status:200, Body: all organization repositories (public
     *         and private)
     */
    @GetMapping(value = "/orgs/{orgName}/repos")
    public ResponseEntity getOrganizationRepositories(@PathVariable String orgName) {
        log.trace("getOrganizationRepositories: orgName={}", orgName);
        AccessTokenDto accessTokenDto = gitHubService.getAccessToken(orgName);
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
        gitHubService.storeSCMOrgToken(scmAccessTokenDto);

        String path = String.format(urlPatternGetOrgRepositories, orgName);
        ResponseEntity<RepositoryDto[]> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.GET,
                                                                           null, null,
                                                                           RepositoryDto[].class, scmAccessTokenDto.getAccessToken());

        ArrayList<RepositoryDto> orgRepositoryDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (RepositoryDto repositoryDto : orgRepositoryDtos) {
            repositoryDto.setWebHookEnabled( isWebHookEnabled(orgName, repositoryDto.getName(),
                                                              accessTokenDto.getAccessToken()));
        }
        gitHubService.storeSCMOrgRepos(scmAccessTokenDto, orgRepositoryDtos);

        return ResponseEntity.ok(orgRepositoryDtos);
    }

    @PostMapping(value = "/repos/{orgName}/{repoName}/webhook")
    public ResponseEntity createWebhook(@PathVariable String orgName, @PathVariable String repoName) {
        log.trace("createWebhook: orgName={}, repoName={}", orgName, repoName);

        SCMAccessTokenDto scmAccessTokenDto = gitHubService.getSCMOrgToken(githubUrl, orgName);
        String path = String.format(urlPatternRepoWebhook, orgName, repoName);
        WebhookDto webhookDto = gitHubService.initWebhook();
        ResponseEntity<WebhookDto> response =  restHelper.sendBearerAuthRequest(path, HttpMethod.POST,
                                                                                webhookDto, null,
                                                                                WebhookDto.class,
                                                                                scmAccessTokenDto.getAccessToken());

        return ResponseEntity.ok(Objects.requireNonNull(response.getBody()));
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
            if (webHookDto.getConfig().getUrl().equals(cxFlowWebHook))
                return webHookDto;
        }
        return null;
    }

    private boolean isWebHookEnabled(String orgName, String repoName, String accessToken) {
        WebhookDto webHookDto = getRepositoryCXFlowWebHook(orgName, repoName, accessToken);
        return webHookDto != null && webHookDto.getActive();
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
        SCMDto scmDto = gitHubService.getScm(githubUrl);
        if (scmDto == null || StringUtils.isEmpty(scmDto.getClientId()) || StringUtils.isEmpty(scmDto.getClientSecret())){
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


}
