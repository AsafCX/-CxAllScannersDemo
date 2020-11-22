package com.checkmarx.controller;

import com.checkmarx.controller.exception.DataStoreException;
import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.datastore.*;
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

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@RestController
@Qualifier("dataStoreController")
public class DataStoreController implements DataController {

    @Value("${data.store}")
    private String dataStoreBase;

    private String urlPatternDataSourceSaveScmOrgToken;

    private String urlPatternDataSourceGetScmOrgToken;

    private String urlPatternDataSourceStoreScm;

    private String urlPatternDataSourceGetScm;

    private String urlPatternDataSourceRepos;

    private String urlPatternDataSourceGetScmOrgRepos;

    private String urlPatternDataSourceGetScmOrgRepo;

    private String urlPatternDataSourceScmOrg;

    private String urlPatternDataSourceScmOrgProperties;
    

    @Autowired
    RestHelper restHelper;


    @PostConstruct
    private void initMembers() {
        urlPatternDataSourceSaveScmOrgToken = dataStoreBase + "/tokens/storeScmAccessToken";

        urlPatternDataSourceGetScmOrgToken = dataStoreBase + "/tokens?scmUrl=%s&orgName=%s";

        urlPatternDataSourceStoreScm = dataStoreBase + "/scms/storeScm";

        urlPatternDataSourceGetScm = dataStoreBase + "/scms/%s";

        urlPatternDataSourceRepos = dataStoreBase + "/repos";

        urlPatternDataSourceGetScmOrgRepos = dataStoreBase + "/repos?scmBaseUrl=%s&orgName=%s";

        urlPatternDataSourceGetScmOrgRepo = dataStoreBase + "/repos/%s?scmBaseUrl=%s&orgName=%s";

        urlPatternDataSourceScmOrg = dataStoreBase + "/orgs/properties?scmBaseUrl=%s&orgName=%s";

        urlPatternDataSourceScmOrgProperties = dataStoreBase + "/orgs/properties";

    }
    
    @Override
    public void storeScmOrgsToken(@NonNull List<ScmAccessTokenDto> scmAccessTokenDtos) {
        log.trace("storeScmOrgsToken: ScmAccessTokenDto={}", scmAccessTokenDtos);

        try {
            restHelper.sendRequest(urlPatternDataSourceSaveScmOrgToken, HttpMethod.PUT, scmAccessTokenDtos
                    , null , ResponseEntity.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
            throw new DataStoreException(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
        }
        log.debug("Save orgs: {} token passed successfully!", scmAccessTokenDtos);
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
            throw new ScmException(RestHelper.SCM_DETAILS_MISSING + ", Scm details received from DataStore" +
                                              " are empty");
        }
        log.debug("Get from DataStore Scm: {} passed successfully",scmUrl);
        return response.getBody();
    }

    @Override
    public void storeScmOrgRepos(@NonNull OrgReposDto orgReposDto) {
        log.trace("storeScmOrgRepos: ScmRepoDto={}", orgReposDto);

        try {
            restHelper.sendRequest(urlPatternDataSourceRepos, HttpMethod.POST, orgReposDto, null,
                                   ResponseEntity.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.STORE_SCM_ORG_REPOS_FAILURE + " ScmRepoDto={}", orgReposDto);
            throw new DataStoreException(RestHelper.STORE_SCM_ORG_REPOS_FAILURE);
        }
        log.debug("Save Scm: {} Org: {} Repos:{} passed successfully", orgReposDto.getScmUrl(),
                  orgReposDto.getOrgName(), orgReposDto.getRepoList());
    }

    @Override
    public List<RepoDto> getScmOrgRepos(@NonNull String scmUrl, @NonNull String orgName) {
        log.trace("getScmOrgRepos: scmUrl={}, orgName={}", scmUrl, orgName);

        String path = String.format(urlPatternDataSourceGetScmOrgRepos, scmUrl, orgName);
        ResponseEntity<List<RepoDto>> response = null;
        try {
            response = restHelper.sendRequest(path, HttpMethod.GET, null, null, List.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestHelper.GET_ORG_REPOS_FAILURE + " scmUrl={}, orgName={}", scmUrl,
                      orgName);
            throw new DataStoreException(RestHelper.GET_ORG_REPOS_FAILURE + " scmUrl="+ scmUrl +
                                                 ", orgName="+ orgName);
        }
        log.debug("Get from DataStore Scm: {} Org: {} Repos: {} passed successfully", scmUrl,
                  orgName, response.getBody());
        return response.getBody();
    }

    @Override
    public RepoDto getScmOrgRepo(@NonNull String scmUrl, @NonNull String orgName,
                                 @NonNull String repoName) {
        log.trace("getScmOrgRepo: scmUrl={}, orgName={}, repoName={}", scmUrl, orgName, repoName);

        String path = String.format(urlPatternDataSourceGetScmOrgRepo, repoName, scmUrl, orgName);
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
        log.debug("Get from DataStore Scm: {} Org: {} Repo: {} passed successfully", scmUrl,
                  orgName, repoName);
        return responseEntity.getBody();
    }

    @Override
    public void updateScmOrgRepo(@NonNull OrgReposDto orgReposDto) {
        log.trace("updateScmOrgRepo: SCMRepoDto={}", orgReposDto);

        try {
            restHelper.sendRequest(urlPatternDataSourceRepos, HttpMethod.PUT, orgReposDto,
                                   null, OrgReposDto.class);
        }  catch(HttpClientErrorException ex){
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestHelper.MISSING_ORG_REPO + " orgName={}, repoName={}", orgReposDto.getOrgName(),
                          orgReposDto.getRepoList());
                throw new DataStoreException(RestHelper.MISSING_ORG_REPO);
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestHelper.UPDATE_ORG_REPO_FAILURE + " orgName={}, repoName={}", orgReposDto
                                  .getOrgName(),
                          orgReposDto.getRepoList());
                throw new DataStoreException(RestHelper.GET_ORG_REPO_FAILURE);
            }
        }
        log.debug("Update in DataStore Scm: {} Org: {} Repo: {} passed successfully",
                  orgReposDto.getScmUrl(), orgReposDto.getOrgName(), orgReposDto.getRepoList());
    }

    @Override
    public OrgPropertiesDto getScmOrgSettings(@NonNull String scmUrl, @NonNull String orgName) {
        log.trace("getScmOrgSettings: scmUrl={}, orgName:{}", scmUrl, orgName);

        ResponseEntity<OrgPropertiesDto> responseEntity = null;
        String path = String.format(urlPatternDataSourceScmOrg, scmUrl, orgName);
        try {
            responseEntity = restHelper.sendRequest(path, HttpMethod.GET, null, null, OrgPropertiesDto.class);
        }  catch(HttpClientErrorException ex){
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestHelper.MISSING_SCM_ORG + " scmUrl={}, orgName={}", scmUrl, orgName);
                throw new DataStoreException(RestHelper.MISSING_SCM_ORG);
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestHelper.GET_SCM_ORG_PROPERTIES_FAILURE + " scmUrl={}, orgName={}",
                          scmUrl, orgName);
                throw new DataStoreException(RestHelper.GET_SCM_ORG_PROPERTIES_FAILURE);
            }
        }
        return responseEntity.getBody();
    }

    @Override
    public void storeScmOrgSettings(@NonNull OrgPropertiesDto orgPropertiesDto) {
        log.trace("storeScmOrgSettings: CxFlowPropertiesDto={}", orgPropertiesDto);

        try {
            restHelper.sendRequest(urlPatternDataSourceScmOrgProperties, HttpMethod.POST,
                                   orgPropertiesDto,
                                   null, OrgPropertiesDto.class);
        }  catch(HttpClientErrorException ex){
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestHelper.MISSING_SCM_ORG + " scmType={}, orgName={}",
                          orgPropertiesDto.getScmUrl(),
                          orgPropertiesDto.getOrgName());
                throw new DataStoreException(RestHelper.MISSING_SCM_ORG);
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestHelper.STORE_SCM_ORG_PROPERTIES_FAILURE + " scmType={}, orgName={}"
                        , orgPropertiesDto.getScmUrl(),
                          orgPropertiesDto.getOrgName());
                throw new DataStoreException(RestHelper.STORE_SCM_ORG_PROPERTIES_FAILURE);
            }
        }

        log.info("Update org settings: {} in DataStore passed successfully", orgPropertiesDto);
    }
}
