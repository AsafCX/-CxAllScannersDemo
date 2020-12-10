package com.checkmarx.service;

import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.utils.RestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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

    

}
