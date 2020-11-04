package com.checkmarx.controller;

import com.checkmarx.controller.exception.DataStoreException;
import com.checkmarx.controller.exception.GitHubException;
import com.checkmarx.dto.SCMAccessTokenDto;
import com.checkmarx.dto.SCMDto;
import com.checkmarx.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

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

    @Autowired
    RestHelper restHelper;

    public void saveSCMOrgToken(SCMAccessTokenDto scmAccessToken) {
        HttpHeaders headers = restHelper.createHeaders(null);
        final HttpEntity<String> request = restHelper.createRequest(scmAccessToken, headers);
        ResponseEntity response =
                restHelper.sendRequest(urlPatternDataSourceSaveSCMOrgToken, HttpMethod.POST, request,
                                                                           ResponseEntity.class);
        if ( !response.getStatusCode().equals(HttpStatus.OK)){
            log.error(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
            throw new DataStoreException(RestHelper.SAVE_ACCESS_TOKEN_FAILURE);
        }
    }

    public SCMAccessTokenDto getSCMOrgToken(String scmUrl, String orgName) {
        HttpHeaders headers = restHelper.createHeaders(null);
        final HttpEntity<String> request = restHelper.createRequest(null, headers);
        String path = String.format(urlPatternDataSourceGetSCMOrgToken, scmUrl, orgName);
        ResponseEntity<SCMAccessTokenDto> response =
                restHelper.sendRequest(path, HttpMethod.GET, request,
                                       SCMAccessTokenDto.class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            log.error(RestHelper.SCM_ORG_TOKEN_MISSING + "\nScm: {}, orgName: {}", scmUrl, orgName);
            throw new DataStoreException(RestHelper.SCM_ORG_TOKEN_MISSING);
        }
        return response.getBody();
    }



    public void storeScm(SCMDto scmDto) {
        HttpHeaders headers = restHelper.createHeaders(null);
        final HttpEntity<String> request = restHelper.createRequest(scmDto, headers);
        ResponseEntity response =
                restHelper.sendRequest(urlPatternDataSourceStoreSCM, HttpMethod.POST, request,
                                       ResponseEntity.class);
        if ( !response.getStatusCode().equals(HttpStatus.OK)){
            log.error(RestHelper.STORE_SCM_FAILURE);
            throw new DataStoreException(RestHelper.STORE_SCM_FAILURE);
        }
    }

    public SCMDto getScm(String scmUrl) {
        HttpHeaders headers = restHelper.createHeaders(null);
        final HttpEntity<String> request = restHelper.createRequest(null, headers);
        String path = String.format(urlPatternDataSourceGetSCM, scmUrl);
        ResponseEntity<SCMDto> response =
                restHelper.sendRequest(path, HttpMethod.GET, request,
                                       SCMDto.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error(RestHelper.SCM_DETAILS_MISSING + "\nScm: {}", scmUrl);
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
        return response.getBody();
    }
}
