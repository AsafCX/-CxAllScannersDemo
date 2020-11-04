package com.checkmarx.controller;

import com.checkmarx.dto.SCMAccessTokenDto;
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

        GitHubConfigDto config = new GitHubConfigDto(clientId, scope);
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
        ResponseEntity failureResponse = verifyAccessToken(accessToken);
        if (failureResponse != null) return failureResponse;

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
        if (accessToken == null || accessToken.getAccessToken().isEmpty()) {
            log.error(RestHelper.ACCESS_TOKEN_MISSING);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(RestHelper.ACCESS_TOKEN_MISSING);
        }
        SCMAccessTokenDto scmAccessTokenDto = new SCMAccessTokenDto(githubUrl, orgName,
                                                                    accessToken.getAccessToken(),
                                                                    TokenType.ACCESS.getType());
        boolean success = dataStoreController.saveSCMOrgToken(scmAccessTokenDto);
        if (!success){
            log.error(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
        }
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
        String path = String.format(urlPatternGenerateOAuthToken, clientId, clientSecret, oAuthCode);
        ResponseEntity<AccessTokenDto> response =  restHelper.sendRequest(path, HttpMethod.POST, null, AccessTokenDto.class);
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
     * token created without GitHub validation
     *
     * @param oAuthToken access token generated before using GitHub api, Gives access to relevant
     *                  GitHub data
     * @return null if verification passed successfully else ResponseEntity with http status:
     *         417, Body: generate token failure string
     */
    private ResponseEntity verifyAccessToken(AccessTokenDto oAuthToken) {
        if (oAuthToken == null || oAuthToken.getAccessToken() == null || oAuthToken.getAccessToken().isEmpty()) {
            log.error(RestHelper.GENERATE_ACCESS_TOKEN_FAILURE);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(RestHelper.GENERATE_ACCESS_TOKEN_FAILURE);
        }else
            return null;
    }


}
