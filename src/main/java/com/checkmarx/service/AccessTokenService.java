package com.checkmarx.service;

import com.checkmarx.controller.exception.DataStoreException;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.internal.TokenInfoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccessTokenService {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String BAD_FORMAT = "Unexpected access token format.";

    private final DataService dataService;

    public TokenInfoDto getTokenInfo(String scmUrl, String orgId) {
        TokenInfoDto result;
        ScmAccessTokenDto tokenInfoFromDataStore = dataService.getSCMOrgToken(scmUrl, orgId);
        String rawToken = tokenInfoFromDataStore.getAccessToken();
        try {
            result = convertToDto(rawToken);
        } catch (JsonProcessingException e) {
            throw getWrapperException(e, rawToken);
        }
        return result;
    }

    private static TokenInfoDto convertToDto(String rawToken) throws JsonProcessingException {
        JsonNode tokenJson = objectMapper.readTree(rawToken);

        // Initialize well-known result properties from the token JSON...
        TokenInfoDto result = objectMapper.treeToValue(tokenJson, TokenInfoDto.class);

        // ...and put the rest of the JSON into additional data.
        result.setAdditionalData(getAdditionalData(tokenJson));

        return result;
    }

    private static ObjectNode getAdditionalData(JsonNode tokenJson) {
        if (tokenJson instanceof ObjectNode) {
            ObjectNode tokenJsonObj = (ObjectNode) tokenJson;
            tokenJsonObj.remove(TokenInfoDto.FIELD_ACCESS_TOKEN);
            tokenJsonObj.remove(TokenInfoDto.FIELD_REFRESH_TOKEN);
            return tokenJsonObj;
        } else {
            log.error("Expected token JSON to be a {}, but it is actually a {}",
                    ObjectNode.class.getSimpleName(),
                    tokenJson.getClass().getSimpleName());

            throw new DataStoreException(BAD_FORMAT);
        }
    }

    private static RuntimeException getWrapperException(Exception cause, String rawTokenFromDataStore) {
        String message = String.format("Error parsing access token as JSON. Value from DataStore: [%s]",
                rawTokenFromDataStore);

        log.error(message, cause);

        return new DataStoreException(BAD_FORMAT, cause);
    }

    public void storeTokenInfo(TokenInfoDto tokenInfo) {
        // TODO: implement
    }
}
