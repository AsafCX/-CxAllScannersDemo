package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.utils.RestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;

@Slf4j
public abstract class AbstractScmService {

    @Value("${cxflow.webhook.url}")
    protected String cxFlowWebHook;
    
    @Autowired
    protected RestWrapper restWrapper;

    @Autowired
    protected DataService dataStoreService;
    
    /**
     * verifyAccessToken method used to verify access token creation, Currently checks if access
     * token created(not null or empty) without GitHub validation
     *
     * @param accessToken access token generated before using GitHub api, Gives access to relevant
     *                  GitHub data
     * @return true if verification passed successfully
     */
    protected static boolean verifyAccessToken(AccessTokenDto accessToken) {
        return accessToken != null && accessToken.getAccessToken() != null && !accessToken.getAccessToken().isEmpty();
    }

    public static ResponseEntity<AccessTokenDto> generateAccessToken(RestWrapper restWrapper, String path, Map<String, String> headers) {
        ResponseEntity<AccessTokenDto> response = restWrapper.sendRequest(path, HttpMethod.POST,
                null, headers,
                AccessTokenDto.class);

        if(!verifyAccessToken(response.getBody())){
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return response;
    }

    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitHub api
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitHub
     */
    protected AccessTokenDto generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseUrl());
        String path = getPath(oAuthCode, scmDto);
        Map<String, String> headers = getHeaders();
        ResponseEntity<AccessTokenDto> response = generateAccessToken(restWrapper, path, headers);
        return response.getBody();
    }

    public abstract String getBaseUrl() ;

    protected abstract Map<String, String> getHeaders() ;

    protected abstract String getPath(String oAuthCode, ScmDto scmDto);
}
