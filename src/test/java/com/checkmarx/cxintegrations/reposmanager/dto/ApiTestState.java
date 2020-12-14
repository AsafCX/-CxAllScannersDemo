package com.checkmarx.cxintegrations.reposmanager.dto;

import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * State shared between several Step classes.
 */
@Component
@Getter
@Setter
@Slf4j
public class ApiTestState {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<String> lastResponse;

    private OrgSettingsWebDto requestForSending;

    private final List<String> expectedFieldNames = new ArrayList<>();

    public void setLastResponse(ResponseEntity<String> value) {
        log.info("Saving response into the test state.");
        lastResponse = value;
    }

    public ResponseEntity<JsonNode> getLastResponseAsJson() {
        ResponseEntity<JsonNode> result = null;
        try {
            if (lastResponse != null) {
                result = new ResponseEntity<>(objectMapper.readTree(lastResponse.getBody()), lastResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Unable to read response body as JSON.", e);
        }
        return result;
    }

    public void setRequestForSending(OrgSettingsWebDto value) {
        log.info("Setting a request to send.");
        requestForSending = value;
    }

    public void prepareForRequestSending() {
        log.info("Clearing the expected field name list.");
        expectedFieldNames.clear();

        log.info("Forgetting the last response.");
        lastResponse = null;
    }

    public void clear() {
        log.info("Clearing the test state.");
        lastResponse = null;
        requestForSending = null;
        expectedFieldNames.clear();
    }
}
