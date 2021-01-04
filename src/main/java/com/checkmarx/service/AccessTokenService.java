package com.checkmarx.service;

import com.checkmarx.controller.exception.DataStoreException;
import com.checkmarx.dto.datastore.ScmAccessTokenDto2;
import com.checkmarx.dto.datastore.TokenInfoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Compatibility layer between token representation in DataStore and the rest of the code that uses SCM access tokens.
 */
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
        ScmAccessTokenDto2 tokenInfoFromDataStore = dataService.getTokenInfo(scmUrl, orgId);
        String rawToken = tokenInfoFromDataStore.getAccessToken();
        try {
            result = convertToDto(tokenInfoFromDataStore);
        } catch (JsonProcessingException e) {
            throw getWrapperException(e, rawToken);
        }
        return result;
    }

    public long createTokenInfo(TokenInfoDto tokenInfo) {
        ScmAccessTokenDto2 dsTokenInfo = getTokenInfoForDataStore(tokenInfo);
        return dataService.createTokenInfo(dsTokenInfo);
    }

    public void updateTokenInfo(TokenInfoDto tokenInfo) {
        ScmAccessTokenDto2 dsTokenInfo = getTokenInfoForDataStore(tokenInfo);
        dataService.updateTokenInfo(dsTokenInfo);
    }

    private ScmAccessTokenDto2 getTokenInfoForDataStore(TokenInfoDto tokenInfo) {
        Map<String, String> dataForSaving = new HashMap<>(tokenInfo.getAdditionalData());
        dataForSaving.put(TokenInfoDto.FIELD_ACCESS_TOKEN, tokenInfo.getAccessToken());
        dataForSaving.put(TokenInfoDto.FIELD_REFRESH_TOKEN, tokenInfo.getRefreshToken());

        String rawToken = convertMapToJsonString(dataForSaving);

        return ScmAccessTokenDto2.builder()
                .id(tokenInfo.getId())
                .accessToken(rawToken)
                .build();
    }

    private static String convertMapToJsonString(Map<String, String> source) {
        String rawToken;
        try {
            rawToken = objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            log.error("Error serializing token additional data into JSON.", e);
            throw new DataStoreException(BAD_FORMAT, e);
        }
        return rawToken;
    }

    private static TokenInfoDto convertToDto(ScmAccessTokenDto2 tokenInfoFromDataStore) throws JsonProcessingException {
        JsonNode tokenJson = objectMapper.readTree(tokenInfoFromDataStore.getAccessToken());

        // Initialize well-known result properties from the token JSON...
        TokenInfoDto result = objectMapper.treeToValue(tokenJson, TokenInfoDto.class);

        // ...and put the rest of the JSON into additional data.
        result.getAdditionalData().putAll(getAdditionalData(tokenJson));

        result.setId(tokenInfoFromDataStore.getId());

        return result;
    }

    private static Map<String, String> getAdditionalData(JsonNode tokenJson) {
        Map<String, String> result;
        if (tokenJson instanceof ObjectNode) {
            result = fieldsToMap((ObjectNode) tokenJson);
        } else {
            log.error("Expected token JSON to be a {}, but it is actually a {}",
                    ObjectNode.class.getSimpleName(),
                    tokenJson.getClass().getSimpleName());

            throw new DataStoreException(BAD_FORMAT);
        }
        return result;
    }

    private static Map<String, String> fieldsToMap(ObjectNode source) {
        Map<String, String> result = new HashMap<>();

        // Prevent any side effects.
        ObjectNode sourceClone = source.deepCopy();

        sourceClone.remove(TokenInfoDto.FIELD_ACCESS_TOKEN);
        sourceClone.remove(TokenInfoDto.FIELD_REFRESH_TOKEN);

        // Assuming the JSON object is flat (no inner objects).
        sourceClone.fields().forEachRemaining(addToMap(result));

        return result;
    }

    private static Consumer<Map.Entry<String, JsonNode>> addToMap(Map<String, String> target) {
        return field -> target.put(field.getKey(), field.getValue().asText());
    }

    private static RuntimeException getWrapperException(Exception cause, String rawTokenFromDataStore) {
        String message = String.format("Error parsing access token as JSON. Value from DataStore: [%s]",
                rawTokenFromDataStore);

        log.error(message, cause);

        return new DataStoreException(BAD_FORMAT, cause);
    }
}
