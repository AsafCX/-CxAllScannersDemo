package com.checkmarx.controller;

import com.checkmarx.controller.exception.GitHubException;
import com.checkmarx.dto.SCMAccessTokenDto;
import com.checkmarx.dto.SCMDto;
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

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.scope}")
    private String scope;

    @Value("${github.url}")
    private String githubUrl;

    @Autowired
    DataStoreController dataStoreController;

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

        //TODO Should be remove from env variables
        GitHubConfigDto config = new GitHubConfigDto(clientId, scope);
        SCMDto scmDto = new SCMDto(githubUrl, clientId, clientSecret);
        dataStoreController.storeScm(scmDto);
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
        HttpHeaders headers = restHelper.createHeaders(null);
        headers.setBearerAuth(userAccessToken);
        final HttpEntity<String> request = restHelper.createRequest(null, headers);
        ResponseEntity<RepositoryDto[]> response =
                restHelper.sendRequest(urlPatternGetUserRepositories,
                                                                HttpMethod.GET, request, RepositoryDto[].class);
        ArrayList<RepositoryDto> userRepositoryDtos = new ArrayList<>(Arrays.asList(response.getBody()));
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
        AccessTokenDto accessToken = gitHubService.getAccessToken(orgName);
        if (!verifyAccessToken(accessToken)) {
            log.error(RestHelper.ACCESS_TOKEN_MISSING);
            throw new GitHubException(RestHelper.ACCESS_TOKEN_MISSING);
        }
        SCMAccessTokenDto scmAccessTokenDto = new SCMAccessTokenDto(githubUrl, orgName,
                                                                    accessToken.getAccessToken(),
                                                                    TokenType.ACCESS.getType());
        dataStoreController.saveSCMOrgToken(scmAccessTokenDto);
        HttpHeaders headers = restHelper.createHeaders(null);
        headers.setBearerAuth(accessToken.getAccessToken());
        final HttpEntity<String> request = restHelper.createRequest(null, headers);
        String path = String.format(urlPatternGetOrgRepositories, orgName);
        ResponseEntity<RepositoryDto[]> response =  restHelper.sendRequest(path, HttpMethod.GET, request, RepositoryDto[].class);
        ArrayList<RepositoryDto> orgRepositoryDtos = new ArrayList<>(Arrays.asList(response.getBody()));
        return ResponseEntity.ok(orgRepositoryDtos);
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
        SCMDto scmDto = dataStoreController.getScm(githubUrl);
        if (scmDto == null || StringUtils.isEmpty(scmDto.getClientId()) || StringUtils.isEmpty(scmDto.getClientSecret())){
            log.error(RestHelper.SCM_DETAILS_MISSING);
            throw new GitHubException(RestHelper.SCM_DETAILS_MISSING);
        }
        String path = String.format(urlPatternGenerateOAuthToken, scmDto.getClientId(),
                                    scmDto.getClientSecret(),
                                    oAuthCode);
        ResponseEntity<AccessTokenDto> response =  restHelper.sendRequest(path, HttpMethod.POST, null, AccessTokenDto.class);
        if(!response.getStatusCode().equals(HttpStatus.OK) || !verifyAccessToken(response.getBody())){
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
        HttpHeaders headers = restHelper.createHeaders(null);
        headers.setBearerAuth(accessToken);
        final HttpEntity<String> request = restHelper.createRequest(null, headers);
        ResponseEntity<OrganizationDto[]> response =
                restHelper.sendRequest(urlPatternGetUserOrganizations, HttpMethod.GET, request, OrganizationDto[].class);
        return new ArrayList<>(Arrays.asList(response.getBody()));
    }

    /**
     * verifyAccessToken method used to verify access token creation, Currently checks if access
     * token created(not null or empty) without GitHub validation
     *
     * @param accessToken access token generated before using GitHub api, Gives access to relevant
     *                  GitHub data
     * @return null if verification passed successfully else ResponseEntity with http status:
     *         417, Body: generate token failure string
     */
    private boolean verifyAccessToken(AccessTokenDto accessToken) {
        return accessToken != null && accessToken.getAccessToken() != null && !accessToken.getAccessToken().isEmpty();
    }


}
