package com.checkmarx.cxintegrations.reposmanager.dto;

import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.fasterxml.jackson.databind.JsonNode;
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
    private ResponseEntity<JsonNode> lastResponse;

    private OrgSettingsWebDto requestForSending;

    private final List<String> expectedFieldNames = new ArrayList<>();

    public void setLastResponse(ResponseEntity<JsonNode> value) {
        log.info("Saving response into the test state.");
        lastResponse = value;
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
