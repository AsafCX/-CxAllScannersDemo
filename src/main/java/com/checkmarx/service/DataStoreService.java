package com.checkmarx.service;

import com.checkmarx.controller.exception.DataStoreException;
import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Qualifier("dataStoreService")
public class DataStoreService implements DataService {

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
    RestWrapper restWrapper;


    @PostConstruct
    private void initMembers() {
        urlPatternDataSourceSaveScmOrgToken = dataStoreBase + "/tokens/storeScmAccessToken";

        urlPatternDataSourceGetScmOrgToken = dataStoreBase + "/tokens?scmUrl=%s&orgIdentity=%s";

        urlPatternDataSourceStoreScm = dataStoreBase + "/scms/storeScm";

        urlPatternDataSourceGetScm = dataStoreBase + "/scms/%s";

        urlPatternDataSourceRepos = dataStoreBase + "/repos";

        urlPatternDataSourceGetScmOrgRepos = dataStoreBase + "/repos?scmBaseUrl=%s&orgIdentity=%s";

        urlPatternDataSourceGetScmOrgRepo = dataStoreBase + "/repos/%s?scmBaseUrl=%s&orgIdentity=%s";

        urlPatternDataSourceScmOrg = dataStoreBase + "/orgs/properties?scmBaseUrl=%s&orgIdentity=%s";

        urlPatternDataSourceScmOrgProperties = dataStoreBase + "/orgs/properties";

    }
    
    @Override
    public void storeScmOrgsToken(@NonNull List<ScmAccessTokenDto> scmAccessTokenDtos) {
        log.trace("storeScmOrgsToken: ScmAccessTokenDto={}", scmAccessTokenDtos);

        try {
            restWrapper.sendRequest(urlPatternDataSourceSaveScmOrgToken, HttpMethod.PUT, scmAccessTokenDtos
                    , null , ResponseEntity.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestWrapper.SAVE_ACCESS_TOKEN_FAILURE);
            throw new DataStoreException(RestWrapper.SAVE_ACCESS_TOKEN_FAILURE, ex);
        }
        log.debug("Save orgs: {} token passed successfully!", scmAccessTokenDtos);
    }

    @Override
    public ScmAccessTokenDto getSCMOrgToken(@NonNull String scmUrl, @NonNull String orgIdentity) {
        log.trace("getScmOrgToken: scmUrl={}, orgIdentity={}", scmUrl, orgIdentity);

        String path = String.format(urlPatternDataSourceGetScmOrgToken, scmUrl, orgIdentity);

        ResponseEntity<ScmAccessTokenDto> response = null;
        try {
            response = restWrapper.sendRequest(path, HttpMethod.GET, null, null,
                                               ScmAccessTokenDto.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestWrapper.SCM_ORG_TOKEN_MISSING + " Scm: {}, orgId: {}", scmUrl,
                      orgIdentity);
            throw new DataStoreException(RestWrapper.SCM_ORG_TOKEN_MISSING, ex);
        }
        log.debug("Get from DataStore token for Scm: {} Org: {} successfully!", scmUrl, orgIdentity);
        return response.getBody();
    }

    @Override
    public void storeScm(@NonNull ScmDto scmDto) {
        log.trace("storeScm: ScmDto={}", scmDto);

        try {
            restWrapper.sendRequest(urlPatternDataSourceStoreScm, HttpMethod.POST, scmDto, null,
                                    ResponseEntity.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestWrapper.STORE_SCM_FAILURE + " ScmDto={}", scmDto);
            throw new DataStoreException(RestWrapper.STORE_SCM_FAILURE, ex);
        }
        log.debug("Save Scm: {} passed successfully", scmDto.getBaseUrl());
    }

    @Override
    public ScmDto getScm(@NonNull String scmUrl) {
        log.trace("getScm: scmUrl={}", scmUrl);

        String path = String.format(urlPatternDataSourceGetScm, scmUrl);

        ResponseEntity<ScmDto> response = null;
        try {
            response = restWrapper.sendRequest(path, HttpMethod.GET, null, null,
                                               ScmDto.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestWrapper.SCM_DETAILS_MISSING + " for requested Scm: {}", scmUrl);
            throw new DataStoreException(RestWrapper.SCM_DETAILS_MISSING + " for requested Scm: " + scmUrl, ex);
        }
        ScmDto scmDto = response.getBody();
        if (scmDto == null || StringUtils
                .isEmpty(scmDto.getClientId()) || StringUtils.isEmpty(scmDto.getClientSecret())){
            log.error(RestWrapper.SCM_DETAILS_MISSING + ", Scm details received from DataStore" +
                              " are empty");
            throw new ScmException(RestWrapper.SCM_DETAILS_MISSING + ", Scm details received from DataStore" +
                                              " are empty");
        }
        log.debug("Get from DataStore Scm: {} passed successfully",scmUrl);
        return response.getBody();
    }

    @Override
    public void storeScmOrgRepos(@NonNull OrgReposDto orgReposDto) {
        log.trace("storeScmOrgRepos: ScmRepoDto={}", orgReposDto);

        try {
            restWrapper.sendRequest(urlPatternDataSourceRepos, HttpMethod.POST, orgReposDto, null,
                                    ResponseEntity.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestWrapper.STORE_SCM_ORG_REPOS_FAILURE + " ScmRepoDto={}", orgReposDto);
            throw new DataStoreException(RestWrapper.STORE_SCM_ORG_REPOS_FAILURE, ex);
        }
        log.debug("Save Scm: {} Org: {} Repos:{} passed successfully", orgReposDto.getScmUrl(),
                  orgReposDto.getOrgIdentity(), orgReposDto.getRepoList());
    }

    @Override
    public List<RepoDto> getScmOrgRepos(@NonNull String scmUrl, @NonNull String orgIdentity) {
        log.trace("getScmOrgRepos: scmUrl={}, orgIdentity={}", scmUrl, orgIdentity);

        String path = String.format(urlPatternDataSourceGetScmOrgRepos, scmUrl, orgIdentity);
        ResponseEntity<List<RepoDto>> response = null;
        try {
            response = restWrapper.sendRequest(path, HttpMethod.GET, null, null, List.class);
        }  catch(HttpClientErrorException ex){
            log.error("HttpClientErrorException: {}", ex.getMessage());
            log.error(RestWrapper.GET_ORG_REPOS_FAILURE + " scmUrl={}, orgIdentity={}", scmUrl,
                      orgIdentity);
            throw new DataStoreException(RestWrapper.GET_ORG_REPOS_FAILURE + " scmUrl="+ scmUrl +
                                                 ", orgIdentity="+ orgIdentity, ex);
        }
        log.debug("Get from DataStore Scm: {} Org: {} Repos: {} passed successfully", scmUrl,
                  orgIdentity, response.getBody());
        return response.getBody();
    }

    @Override
    public RepoDto getScmOrgRepo(@NonNull String scmUrl, @NonNull String orgIdentity,
                                 @NonNull String repoIdentity) {
        log.trace("getScmOrgRepo: scmUrl={}, orgIdentity={}, repoIdentity={}", scmUrl, orgIdentity,
                  repoIdentity);

        String path = String.format(urlPatternDataSourceGetScmOrgRepo, repoIdentity, scmUrl,
                                    orgIdentity);
        ResponseEntity<RepoDto> responseEntity = null;
        try {
            responseEntity = restWrapper.sendRequest(path, HttpMethod.GET, null, null, RepoDto.class);
        }  catch(HttpClientErrorException ex){
            String exceptionMessage;
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestWrapper.MISSING_ORG_REPO + " orgIdentity={}, repoIdentity={}", orgIdentity,
                          repoIdentity);
                exceptionMessage = RestWrapper.MISSING_ORG_REPO;
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestWrapper.GET_ORG_REPO_FAILURE + " orgIdentity={}, repoIdentity={}",
                          orgIdentity,
                          repoIdentity);
                exceptionMessage = RestWrapper.GET_ORG_REPO_FAILURE;
            }
            throw new DataStoreException(exceptionMessage, ex);
        }
        log.debug("Get from DataStore Scm: {} Org: {} Repo: {} passed successfully", scmUrl,
                  orgIdentity, repoIdentity);
        return responseEntity.getBody();
    }

    @Override
    public void updateScmOrgRepo(@NonNull OrgReposDto orgReposDto) {
        log.trace("updateScmOrgRepo: SCMRepoDto={}", orgReposDto);

        try {
            restWrapper.sendRequest(urlPatternDataSourceRepos, HttpMethod.PUT, orgReposDto,
                                    null, OrgReposDto.class);
        }  catch(HttpClientErrorException ex){
            String exceptionMessage;
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestWrapper.MISSING_ORG_REPO + " orgIdentity={}, repoIdentity={}", orgReposDto.getOrgIdentity(),
                          orgReposDto.getRepoList());
                exceptionMessage = RestWrapper.MISSING_ORG_REPO;
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestWrapper.UPDATE_ORG_REPO_FAILURE + " orgIdentity={}, repoIdentity={}", orgReposDto
                                  .getOrgIdentity(),
                          orgReposDto.getRepoList());
                exceptionMessage = RestWrapper.GET_ORG_REPO_FAILURE;
            }
            throw new DataStoreException(exceptionMessage, ex);
        }
        log.debug("Update in DataStore Scm: {} Org: {} Repo: {} passed successfully",
                  orgReposDto.getScmUrl(), orgReposDto.getOrgIdentity(), orgReposDto.getRepoList());
    }

    @Override
    public OrgPropertiesDto getScmOrgSettings(@NonNull String scmUrl, @NonNull String orgIdentity) {
        log.trace("getScmOrgSettings: scmUrl={}, orgIdentity:{}", scmUrl, orgIdentity);

        ResponseEntity<OrgPropertiesDto> responseEntity = null;
        String path = String.format(urlPatternDataSourceScmOrg, scmUrl, orgIdentity);
        try {
            responseEntity = restWrapper
                    .sendRequest(path, HttpMethod.GET, null, null, OrgPropertiesDto.class);
        }  catch(HttpClientErrorException ex){
            String exceptionMessage ;
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestWrapper.MISSING_SCM_ORG + " scmUrl={}, orgIdentity={}", scmUrl,
                          orgIdentity);
                exceptionMessage = RestWrapper.MISSING_SCM_ORG;
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestWrapper.GET_SCM_ORG_PROPERTIES_FAILURE + " scmUrl={}, orgIdentity={}",
                          scmUrl, orgIdentity);
                exceptionMessage = RestWrapper.GET_SCM_ORG_PROPERTIES_FAILURE;
            }
            throw new DataStoreException(exceptionMessage, ex);
        }
        return responseEntity.getBody();
    }

    @Override
    public void storeScmOrgSettings(@NonNull OrgPropertiesDto orgPropertiesDto) {
        log.trace("storeScmOrgSettings: CxFlowPropertiesDto={}", orgPropertiesDto);

        try {
            restWrapper.sendRequest(urlPatternDataSourceScmOrgProperties, HttpMethod.POST,
                                    orgPropertiesDto,
                                    null, OrgPropertiesDto.class);
        }  catch(HttpClientErrorException ex){
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error(RestWrapper.MISSING_SCM_ORG + " scmType={}, orgIdentity={}",
                          orgPropertiesDto.getScmUrl(),
                          orgPropertiesDto.getOrgIdentity());
                throw new DataStoreException(RestWrapper.MISSING_SCM_ORG, ex);
            } else {
                log.error("HttpClientErrorException: {}", ex.getMessage());
                log.error(RestWrapper.STORE_SCM_ORG_PROPERTIES_FAILURE + " scmType={}, orgIdentity={}"
                        , orgPropertiesDto.getScmUrl(),
                          orgPropertiesDto.getOrgIdentity());
                throw new DataStoreException(RestWrapper.STORE_SCM_ORG_PROPERTIES_FAILURE, ex);
            }
        }

        log.info("Update org settings: {} in DataStore passed successfully", orgPropertiesDto);
    }

    @Override
    public void updateWebhook(@NonNull String repoId, ScmAccessTokenDto scmAccessTokenDto, String webhookId, Boolean isWebhook ) {
        RepoDto repoDto = RepoDto.builder().repoIdentity(repoId).webhookId(webhookId).isWebhookConfigured(isWebhook).build();
        updateScmOrgRepo(OrgReposDto.builder()
                .orgIdentity(scmAccessTokenDto.getOrgIdentity())
                .scmUrl(scmAccessTokenDto.getScmUrl())
                .repoList(Collections.singletonList(repoDto))
                .build());
    }
}
