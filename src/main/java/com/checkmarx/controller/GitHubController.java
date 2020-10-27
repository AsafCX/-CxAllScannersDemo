package com.checkmarx.controller;

import com.checkmarx.dto.github.OrganizationDto;
import com.checkmarx.dto.github.RepositoryDto;
import com.checkmarx.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/github")
public class GitHubController {

    @Autowired
    RestTemplate restTemplate;

    @GetMapping(value="/user/repos")
    public ResponseEntity<List<RepositoryDto>> getUserRepositories(
            @RequestHeader("UserAuthToken") String userAuthToken) {

        final HttpEntity<String> request = createRequest(null, createHeaders(userAuthToken));
        ResponseEntity<RepositoryDto[]> response = sendRequest(RestHelper.urlPatternGetUserRepositories, HttpMethod.GET, request, RepositoryDto[].class);
        ArrayList<RepositoryDto> userRepositoryDtos = new ArrayList<>(Arrays.asList(response.getBody()));
        return ResponseEntity.status(HttpStatus.OK).body(userRepositoryDtos);

    }

    @GetMapping(value="/user/orgs")
    public ResponseEntity<List<OrganizationDto>> getUserOrganizations(
            @RequestHeader("UserAuthToken") String userAuthToken) {

        final HttpEntity<String> request = createRequest(null, createHeaders(userAuthToken));
        ResponseEntity<OrganizationDto[]> response = sendRequest(RestHelper.urlPatternGetUserOrganizations, HttpMethod.GET, request, OrganizationDto[].class);
        ArrayList<OrganizationDto> userOrganizationDtos = new ArrayList<>(Arrays.asList(response.getBody()));
        return ResponseEntity.status(HttpStatus.OK).body(userOrganizationDtos);
    }


    @GetMapping(value="org/repos")
    public ResponseEntity<List<RepositoryDto>> getOrganizationRepositories(
            @RequestHeader("UserAuthToken") String userAuthToken, @RequestHeader("Org") String orgName) {

        final HttpEntity<String> request = createRequest(null, createHeaders(userAuthToken));
        String path = String.format(RestHelper.urlPatternGetOrgRepositories, orgName);
        ResponseEntity<RepositoryDto[]> response = sendRequest(path, HttpMethod.GET, request, RepositoryDto[].class);
        ArrayList<RepositoryDto> orgRepositoryDtos = new ArrayList<>(Arrays.asList(response.getBody()));

        return ResponseEntity.status(HttpStatus.OK).body(orgRepositoryDtos);


    }

    private HttpHeaders createHeaders(String userAuthToken) {
        final HttpHeaders headers = new HttpHeaders();
        String tokenHeader = String.format(RestHelper.githubTokenPattern, userAuthToken);
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


}
