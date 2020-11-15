package com.checkmarx.controller;

import com.checkmarx.controller.exception.DataStoreException;
import com.checkmarx.controller.exception.GitHubException;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.datastore.ScmRepoDto;
import com.checkmarx.utils.RestHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Slf4j
@RestController
@Qualifier("dataStoreController")
public class DataStoreController implements DataController {

    @Value("${data.source.url.pattern.save.scm.token}")
    private String urlPatternDataSourceSaveScmOrgToken;

    @Value("${data.source.url.pattern.get.scm.token}")
    private String urlPatternDataSourceGetScmOrgToken;

    @Value("${data.source.url.pattern.store.scm}")
    private String urlPatternDataSourceStoreScm;

    @Value("${data.source.url.pattern.get.scm}")
    private String urlPatternDataSourceGetScm;

    @Value("${data.source.url.pattern.repos}")
    private String urlPatternDataSourceRepos;

    @Value("${data.source.url.pattern.get.scm.org.repos}")
    private String urlPatternDataSourceGetScmOrgRepos;

    @Value("${data.source.url.pattern.get.scm.org.repo}")
    private String urlPatternDataSourceGetScmOrgRepo;


    @Autowired
    RestHelper restHelper;

    @Override
    public void storeScmOrgToken(@NonNull ScmAccessTokenDto scmAccessToken) {
        log.trace("storeScmOrgToken: scmAccessToken={}", scmAccessToken);

        try {
            restHelper.sendRequest(urlPatternDataSourceSaveScmOrgToken, HttpMethod.PUT, scmAccessToken
                    , null , ResponseEntity.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
            throw new DataStoreException(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
        }
        log.debug("Save Scm: {} org: {} token passed successfully!", scmAccessToken.getScmUrl(),
                 scmAccessToken.getOrgName());
    }

    @Override
    public ScmAccessTokenDto getSCMOrgToken(@NonNull String scmUrl, @NonNull String orgName) {
        log.trace("getScmOrgToken: scmUrl={}, orgName={}", scmUrl, orgName);

        String path = String.format(urlPatternDataSourceGetScmOrgToken, scmUrl, orgName);

        ResponseEntity<ScmAccessTokenDto> response = null;
        try {
            response = restHelper.sendRequest(path, HttpMethod.GET, null, null,
                                              ScmAccessTokenDto.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.SCM_ORG_TOKEN_MISSING + " Scm: {}, orgName: {}", scmUrl, orgName);
            throw new DataStoreException(RestHelper.SCM_ORG_TOKEN_MISSING);
        }
        log.debug("Get from DataStore token for Scm: {} Org: {} successfully!", scmUrl, orgName);
        return response.getBody();
    }

    @Override
    public void storeScm(@NonNull ScmDto scmDto) {
        log.trace("storeScm: ScmDto={}", scmDto);

        try {
            restHelper.sendRequest(urlPatternDataSourceStoreScm, HttpMethod.POST, scmDto, null,
                                   ResponseEntity.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.STORE_SCM_FAILURE + " ScmDto={}", scmDto);
            throw new DataStoreException(RestHelper.STORE_SCM_FAILURE);
        }
        log.debug("Save Scm: {} passed successfully", scmDto.getBaseUrl());
    }

    @Override
    public ScmDto getScm(@NonNull String scmUrl) {
        log.trace("getScm: scmUrl={}", scmUrl);

        String path = String.format(urlPatternDataSourceGetScm, scmUrl);

        ResponseEntity<ScmDto> response = null;
        try {
            response = restHelper.sendRequest(path, HttpMethod.GET, null, null,
                                              ScmDto.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.SCM_DETAILS_MISSING + " Scm: {}", scmUrl);
            throw new DataStoreException(RestHelper.SCM_DETAILS_MISSING);
        }
        ScmDto scmDto = response.getBody();
        if (scmDto == null || StringUtils
                .isEmpty(scmDto.getClientId()) || StringUtils.isEmpty(scmDto.getClientSecret())){
            log.error(RestHelper.SCM_DETAILS_MISSING + ", Scm details received from DataStore" +
                              " are empty");
            throw new GitHubException(RestHelper.SCM_DETAILS_MISSING + ", Scm details received from DataStore" +
                                              " are empty");
        }
        log.debug("Get from DataStore Scm: {} passed successfully",scmUrl);
        return response.getBody();
    }

    @Override
    public void storeScmOrgRepos(@NonNull ScmRepoDto scmRepoDto) {
        log.trace("storeScmOrgRepos: ScmRepoDto={}", scmRepoDto);

        try {
            restHelper.sendRequest(urlPatternDataSourceRepos, HttpMethod.POST, scmRepoDto, null,
                                   ResponseEntity.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.STORE_SCM_ORG_REPOS_FAILURE + " ScmRepoDto={}", scmRepoDto);
            throw new DataStoreException(RestHelper.STORE_SCM_ORG_REPOS_FAILURE);
        }
        log.debug("Save Scm: {} Org: {} Repos:{} passed successfully", scmRepoDto.getScmUrl(),
                  scmRepoDto.getOrgName(), scmRepoDto.getRepoList());
    }

    @Override
    public List<RepoDto> getScmOrgRepos(@NonNull String baseUrl, @NonNull String orgName) {
        log.trace("getScmOrgRepos: baseUrl={}, orgName={}", baseUrl, orgName);

        String path = String.format(urlPatternDataSourceGetScmOrgRepos, baseUrl, orgName);
        ResponseEntity<List<RepoDto>> response = null;
        try {
            response = restHelper.sendRequest(path, HttpMethod.GET, null, null, List.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.GET_ORG_REPOS_FAILURE + " baseUrl={}, orgName={}", baseUrl,
                      orgName);
            throw new DataStoreException(RestHelper.GET_ORG_REPOS_FAILURE + " baseUrl="+ baseUrl+
                                                 ", orgName="+ orgName);
        }
        log.debug("Get from DataStore Scm: {} Org: {} Repos: {} passed successfully", baseUrl,
                  orgName, response.getBody());
        return response.getBody();
    }

    @Override
    public RepoDto getScmOrgRepo(@NonNull String baseUrl, @NonNull String orgName,
                                 @NonNull String repoName) {
        log.trace("getScmOrgRepo: baseUrl={}, orgName={}, repoName={}", baseUrl, orgName, repoName);

        String path = String.format(urlPatternDataSourceGetScmOrgRepo, repoName, baseUrl, orgName);
        ResponseEntity<RepoDto> responseEntity = null;
        try {
            responseEntity = restHelper.sendRequest(path, HttpMethod.GET, null, null, RepoDto.class);
        }  catch(HttpClientErrorException ex){
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestHelper.MISSING_ORG_REPO + " orgName={}, repoName={}", orgName,
                          repoName);
                throw new DataStoreException(RestHelper.MISSING_ORG_REPO);
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestHelper.GET_ORG_REPO_FAILURE + " orgName={}, repoName={}", orgName,
                          repoName);
                throw new DataStoreException(RestHelper.GET_ORG_REPO_FAILURE);
            }
        }
        log.debug("Get from DataStore Scm: {} Org: {} Repo: {} passed successfully", baseUrl,
                  orgName, repoName);
        return responseEntity.getBody();
    }

    @Override
    public void updateScmOrgRepo(@NonNull ScmRepoDto scmRepoDto) {
        log.trace("updateScmOrgRepo: SCMRepoDto={}", scmRepoDto);

        try {
            restHelper.sendRequest(urlPatternDataSourceRepos, HttpMethod.PUT, scmRepoDto,
                                   null, ScmRepoDto.class);
        }  catch(HttpClientErrorException ex){
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestHelper.MISSING_ORG_REPO + " orgName={}, repoName={}", scmRepoDto.getOrgName(),
                          scmRepoDto.getRepoList());
                throw new DataStoreException(RestHelper.MISSING_ORG_REPO);
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestHelper.UPDATE_ORG_REPO_FAILURE + " orgName={}, repoName={}", scmRepoDto.getOrgName(),
                          scmRepoDto.getRepoList());
                throw new DataStoreException(RestHelper.GET_ORG_REPO_FAILURE);
            }
        }
        log.debug("Update in DataStore Scm: {} Org: {} Repo: {} passed successfully",
                  scmRepoDto.getScmUrl(), scmRepoDto.getOrgName(), scmRepoDto.getRepoList());
    }
}
