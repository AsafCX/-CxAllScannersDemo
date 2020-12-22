package com.checkmarx.utils;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.service.DataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

@Getter
public class AccessTokenManager {
    
    ScmAccessTokenDto dbDto;
    
    public AccessTokenManager(String dbKey, String orgId, DataService dataStoreService){
        dbDto = dataStoreService.getSCMOrgToken(dbKey, orgId);
    }

    private AccessTokenDto getAccessToken(String tokenJson) {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return objectMapper.readValue(tokenJson, AccessTokenDto.class);
        } catch (JsonProcessingException ex){
            throw new ScmException("Unable to Json -> Object");
        }
    }
    
    public String getAccessTokenStr(){
        AccessTokenDto tokenDto = getAccessToken(getAccessTokenJson());
        return tokenDto.getAccessToken();
    }

    public String getAccessTokenJson(){
        return dbDto.getAccessToken();
    }

    public Object getFullAccessToken(Class responseType) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(getAccessTokenJson(), responseType);
        } catch (JsonProcessingException ex){
            throw new ScmException("Unable to Json -> Object");
        }
    }
    
    
}
