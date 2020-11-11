package com.checkmarx.controller;

import com.checkmarx.controller.exception.DataStoreException;
import com.checkmarx.controller.exception.GitHubException;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.SCMAccessTokenDto;
import com.checkmarx.dto.datastore.SCMDto;
import com.checkmarx.dto.datastore.SCMRepoDto;
import com.checkmarx.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Slf4j
@RestController
public class DataStoreController {

    @Value("${data.source.url.pattern.save.scm.token}")
    private String urlPatternDataSourceSaveSCMOrgToken;

    @Value("${data.source.url.pattern.get.scm.token}")
    private String urlPatternDataSourceGetSCMOrgToken;

    @Value("${data.source.url.pattern.store.scm}")
    private String urlPatternDataSourceStoreSCM;

    @Value("${data.source.url.pattern.get.scm}")
    private String urlPatternDataSourceGetSCM;

    @Value("${data.source.url.pattern.repos}")
    private String urlPatternDataSourceRepos;

    @Value("${data.source.url.pattern.get.scm.org.repos}")
    private String urlPatternDataSourceGetSCMOrgRepos;

    @Value("${data.source.url.pattern.get.scm.org.repo}")
    private String urlPatternDataSourceGetSCMOrgRepo;


    @Autowired
    RestHelper restHelper;

    public void storeSCMOrgToken(SCMAccessTokenDto scmAccessToken) {
        log.trace("saveSCMOrgToken: scmAccessToken={}", scmAccessToken);

        ResponseEntity response =
                restHelper.sendRequest(urlPatternDataSourceSaveSCMOrgToken, HttpMethod.PUT, scmAccessToken
                        , null , ResponseEntity.class);
        if ( !response.getStatusCode().equals(HttpStatus.OK)){
            log.error(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
            throw new DataStoreException(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
        }
        log.debug("Save Scm: {} org: {} token passed successfully!", scmAccessToken.getScmUrl(),
                 scmAccessToken.getOrgName());
    }

    public SCMAccessTokenDto getSCMOrgToken(String scmUrl, String orgName) {
        log.trace("getSCMOrgToken: scmUrl={}, orgName={}", scmUrl, orgName);

        String path = String.format(urlPatternDataSourceGetSCMOrgToken, scmUrl, orgName);
        ResponseEntity<SCMAccessTokenDto> response =
                restHelper.sendRequest(path, HttpMethod.GET, null, null,
                                       SCMAccessTokenDto.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error(RestHelper.SCM_ORG_TOKEN_MISSING + " Scm: {}, orgName: {}", scmUrl, orgName);
            throw new DataStoreException(RestHelper.SCM_ORG_TOKEN_MISSING);
        }
        log.debug("Get from DataStore token for Scm: {} Org: {} successfully!", scmUrl, orgName);
        return response.getBody();
    }



    public void storeScm(SCMDto scmDto) {
        log.trace("storeScm: SCMDto={}", scmDto);

        ResponseEntity response =
                restHelper.sendRequest(urlPatternDataSourceStoreSCM, HttpMethod.POST, scmDto, null,
                                       ResponseEntity.class);
        if ( !response.getStatusCode().equals(HttpStatus.OK)){
            log.error(RestHelper.STORE_SCM_FAILURE + " SCMDto={}", scmDto);
            throw new DataStoreException(RestHelper.STORE_SCM_FAILURE);
        }
        log.debug("Save Scm: {} passed successfully", scmDto.getBaseUrl());
    }

    public SCMDto getScm(String scmUrl) {
        log.trace("getScm: scmUrl={}", scmUrl);

        String path = String.format(urlPatternDataSourceGetSCM, scmUrl);
        ResponseEntity<SCMDto> response =
                restHelper.sendRequest(path, HttpMethod.GET, null, null,
                                       SCMDto.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error(RestHelper.SCM_DETAILS_MISSING + " Scm: {}", scmUrl);
            throw new DataStoreException(RestHelper.SCM_DETAILS_MISSING);
        } else {
            SCMDto scmDto = response.getBody();
            if (scmDto == null || StringUtils
                    .isEmpty(scmDto.getClientId()) || StringUtils.isEmpty(scmDto.getClientSecret())){
                log.error(RestHelper.SCM_DETAILS_MISSING + ", Scm details received from DataStore" +
                                  " are empty");
                throw new GitHubException(RestHelper.SCM_DETAILS_MISSING + ", Scm details received from DataStore" +
                                                  " are empty");
            }
        }
        log.debug("Get from DataStore Scm: {} passed successfully",scmUrl);
        return response.getBody();
    }

    public void storeSCMOrgRepos(SCMRepoDto scmRepoDto) {
        log.trace("storeSCMOrgRepos: SCMRepoDto={}", scmRepoDto);

        ResponseEntity<SCMDto> response =
                restHelper.sendRequest(urlPatternDataSourceRepos, HttpMethod.POST, scmRepoDto
                        , null, ResponseEntity.class);
        if ( !response.getStatusCode().equals(HttpStatus.OK)){
            log.error(RestHelper.STORE_SCM_ORG_REPOS_FAILURE + " SCMRepoDto={}", scmRepoDto);
            throw new DataStoreException(RestHelper.STORE_SCM_ORG_REPOS_FAILURE);
        }
        log.debug("Save Scm: {} Org: {} Repos:{} passed successfully", scmRepoDto.getScmUrl(),
                  scmRepoDto.getOrgName(), scmRepoDto.getRepoList());
    }

    public List<RepoDto> getSCMOrgRepos(String baseUrl, String orgName) {
        log.trace("getSCMReposByOrg: baseUrl={}, orgName={}", baseUrl, orgName);

        String path = String.format(urlPatternDataSourceGetSCMOrgRepos, baseUrl, orgName);
        ResponseEntity<List<RepoDto>> response =
                restHelper.sendRequest(path, HttpMethod.GET, null, null, List.class);
        if ( !response.getStatusCode().equals(HttpStatus.OK)){
            log.error(RestHelper.GET_ORG_REPOS_FAILURE + " baseUrl={}, orgName={}", baseUrl, orgName);
            throw new DataStoreException(RestHelper.GET_ORG_REPOS_FAILURE);
        }
        log.debug("Get from DataStore Scm: {} Org: {} Repos: {} passed successfully", baseUrl,
                  orgName, response.getBody());
        return response.getBody();
    }

    public RepoDto getSCMOrgRepo(String baseUrl, String orgName, String repoName) {
        log.trace("getSCMOrgRepo: baseUrl={}, orgName={}, repoName={}", baseUrl, orgName, repoName);

        String path = String.format(urlPatternDataSourceGetSCMOrgRepo, repoName, baseUrl, orgName);
        ResponseEntity<RepoDto> responseEntity = null;
        try {
            responseEntity = restHelper.sendRequest(path, HttpMethod.GET, null, null, RepoDto.class);
        }  catch(HttpClientErrorException ex){
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestHelper.MISSING_ORG_REPO + " orgName={}, repoName={}", orgName,
                          repoName);
                throw new DataStoreException(RestHelper.MISSING_ORG_REPO);
            } else {
                log.error(RestHelper.GET_ORG_REPO_FAILURE + " orgName={}, repoName={}", orgName,
                          repoName);
                throw new DataStoreException(RestHelper.GET_ORG_REPO_FAILURE);
            }
        }
        log.debug("Get from DataStore Scm: {} Org: {} Repo: {} passed successfully", baseUrl,
                  orgName, repoName);
        return responseEntity.getBody();
    }

    public SCMRepoDto updateSCMOrgRepo(SCMRepoDto scmRepoDto) {
        log.trace("updateSCMOrgRepo: SCMRepoDto={}", scmRepoDto);

        ResponseEntity<SCMRepoDto> response =
                restHelper.sendRequest(urlPatternDataSourceRepos, HttpMethod.PUT, scmRepoDto,
                                       null, SCMRepoDto.class);
        log.debug("Update in DataStore Scm: {} Org: {} Repo: {} passed successfully",
                  scmRepoDto.getScmUrl(), scmRepoDto.getOrgName(), scmRepoDto.getRepoList());
        return response.getBody();
    }
}
