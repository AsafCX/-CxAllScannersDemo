package com.checkmarx.controller;

import com.checkmarx.dto.github.GitHubConfigDto;
import com.checkmarx.dto.github.OAuthTokenDto;
import com.checkmarx.dto.github.OrganizationDto;
import com.checkmarx.dto.github.RepositoryDto;
import com.checkmarx.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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

    @Value("${github.token.format}")
    private String githubTokenPattern;

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.scope}")
    private String scope;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    DataSourceController dataSourceController;

    @GetMapping(value="/config")
    public ResponseEntity getGitHubConfig() {

        GitHubConfigDto config = new GitHubConfigDto(clientId, scope);
        return ResponseEntity.status(HttpStatus.OK).body(config);
    }

    @PostMapping(value="/user/orgs")
    public ResponseEntity getOrganizations(@RequestParam(name = "code") String oAuthCode) {

        OAuthTokenDto oAuthToken = generateOAuthToken(oAuthCode);
        ResponseEntity failureResponse = verifyTokenAndSave(oAuthToken);
        if (failureResponse != null) return failureResponse;
        log.info("OAuth token generated successfully");
        ArrayList<OrganizationDto> userOrganizationDtos = getUserOrganizations(oAuthToken.getAccessToken());
        return ResponseEntity.status(HttpStatus.OK).body(userOrganizationDtos);
    }

    @GetMapping(value="/user/repos")
    public ResponseEntity getUserRepositories(
            @RequestHeader("UserAuthToken") String userAuthToken) {

        final HttpEntity<String> request = createRequest(null, createHeaders(userAuthToken));
        ResponseEntity<RepositoryDto[]> response = sendRequest(urlPatternGetUserRepositories, HttpMethod.GET, request, RepositoryDto[].class);
        ArrayList<RepositoryDto> userRepositoryDtos = new ArrayList<>(Arrays.asList(response.getBody()));
        return ResponseEntity.status(HttpStatus.OK).body(userRepositoryDtos);
    }

    @GetMapping(value="org/repos")
    public ResponseEntity getOrganizationRepositories(
            @RequestHeader("UserAuthToken") String userAuthToken, @RequestHeader("Org") String orgName) {

        final HttpEntity<String> request = createRequest(null, createHeaders(userAuthToken));
        String path = String.format(urlPatternGetOrgRepositories, orgName);
        ResponseEntity<RepositoryDto[]> response = sendRequest(path, HttpMethod.GET, request, RepositoryDto[].class);
        ArrayList<RepositoryDto> orgRepositoryDtos = new ArrayList<>(Arrays.asList(response.getBody()));

        return ResponseEntity.status(HttpStatus.OK).body(orgRepositoryDtos);
    }

    private OAuthTokenDto generateOAuthToken(String oAuthCode) {
        String path = String.format(urlPatternGenerateOAuthToken, clientId, clientSecret, oAuthCode);
        ResponseEntity<OAuthTokenDto> response = sendRequest(path, HttpMethod.POST, null, OAuthTokenDto.class);
        return response.getBody();
    }

    private ArrayList<OrganizationDto> getUserOrganizations( String accessToken) {
        final HttpEntity<String> request = createRequest(null, createHeaders(accessToken));
        ResponseEntity<OrganizationDto[]> response = sendRequest(urlPatternGetUserOrganizations, HttpMethod.GET, request, OrganizationDto[].class);
        return new ArrayList<>(Arrays.asList(response.getBody()));
    }

    private HttpHeaders createHeaders(String userAuthToken) {
        final HttpHeaders headers = new HttpHeaders();
        String tokenHeader = String.format(githubTokenPattern, userAuthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", tokenHeader);
        return headers;
    }

    private ResponseEntity sendRequest(String path, HttpMethod method, HttpEntity<String> request, Class responseType) {
        return restTemplate.exchange(path, method, request, responseType);
    }

    private HttpEntity<String> createRequest(Object body, HttpHeaders headers) {
        return new HttpEntity<>((String) body, headers);
    }

    private ResponseEntity verifyTokenAndSave(OAuthTokenDto oAuthToken) {
        if (oAuthToken == null || oAuthToken.getAccessToken() == null || oAuthToken.getAccessToken().isEmpty() ){
            log.error(RestHelper.GENERATE_OAUTH_TOKEN_FAILURE);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(RestHelper.GENERATE_OAUTH_TOKEN_FAILURE);
        }
        boolean success = dataSourceController.saveToken(oAuthToken);
        if (!success){
            log.error(RestHelper.SAVE_OAUTH_FAILURE);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(RestHelper.SAVE_OAUTH_FAILURE);
        }
        return null;
    }


}
