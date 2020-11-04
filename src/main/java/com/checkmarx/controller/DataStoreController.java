package com.checkmarx.controller;

import com.checkmarx.dto.SCMAccessTokenDto;
import com.checkmarx.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DataStoreController {

    @Value("${data.source.url.pattern.save.scm.token}")
    private String urlPatternDataSourceSaveSCMOrgToken;

    @Autowired
    RestHelper restHelper;

    public Boolean saveSCMOrgToken(SCMAccessTokenDto scmAccessToken) {
        HttpHeaders headers = restHelper.createHeaders(null);
        final HttpEntity<String> request = restHelper.createRequest(scmAccessToken, headers);
        ResponseEntity response =
                restHelper.sendRequest(urlPatternDataSourceSaveSCMOrgToken, HttpMethod.POST, request,
                                                                           ResponseEntity.class);
        return response.getStatusCode().equals(HttpStatus.OK);
    }

    public Boolean storeScm(SCMAccessTokenDto scmAccessToken) {
        HttpHeaders headers = restHelper.createHeaders(null);
        final HttpEntity<String> request = restHelper.createRequest(scmAccessToken, headers);
        ResponseEntity response =
                restHelper.sendRequest(urlPatternDataSourceSaveSCMOrgToken, HttpMethod.POST, request,
                                       ResponseEntity.class);
        return response.getStatusCode().equals(HttpStatus.OK);
    }
}
