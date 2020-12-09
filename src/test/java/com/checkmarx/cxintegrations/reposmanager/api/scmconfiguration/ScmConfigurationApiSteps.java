package com.checkmarx.cxintegrations.reposmanager.api.scmconfiguration;

import com.checkmarx.cxintegrations.reposmanager.WebApiRequestSender;
import com.checkmarx.cxintegrations.reposmanager.dto.ApiTestState;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.service.GitHubService;
import com.checkmarx.service.GitLabService;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@RequiredArgsConstructor
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ScmConfigurationApiSteps {
    private final Map<String, ScmDto> miniDataStore = new HashMap<>();

    @LocalServerPort
    private int apiPort;

    @MockBean
    private final RestTemplate restTemplateMock;

    private final ApiTestState testState;
    private final WebApiRequestSender requestSender;
    private final GitHubService gitHubService;
    private final GitLabService gitLabService;

    @Before
    public void useFakeDataStoreResponses() {
        when(restTemplateMock.exchange(contains("/scms/"), eq(HttpMethod.GET), any(), any(Class.class)))
                .thenAnswer(usingMiniDataStore());
    }

    @Given("data store contains {word} and {word} for the {word} SCM")
    public void dataStoreContains(String clientId, String scope, String scmId) {
        String scmBaseUrl = (scmId.equals("github") ? gitHubService.getBaseDbKey() : gitLabService.getBaseDbKey());
        ScmDto scmConfig = ScmDto.builder()
                .clientId(clientId)
                .baseUrl(scmBaseUrl)
                .clientSecret("let it be our secret")
                .build();
        miniDataStore.clear();
        miniDataStore.put(scmBaseUrl, scmConfig);
    }

    @When("API client calls the `get configuration` API for the {word} SCM")
    public void apiClientCallsAPI(String scmId) throws IOException {
        testState.prepareForRequestSending();
        ResponseEntity<JsonNode> response = requestSender.get("{scmId}/config", apiPort, scmId);
        testState.setLastResponse(response);
    }

    @Given("data store does not contain the {word} SCM")
    public void dataStoreDoesNotContainTheSCM(String sdmId) {
        log.info("Clearing the miniDataStore.");
        miniDataStore.clear();
    }

    private Answer<Object> usingMiniDataStore() {
        return invocation -> {
            // Expecting a path like "/scms/github.com"
            String urlPath = invocation.getArgument(0);

            String scmBaseUrl = StringUtils.substringAfterLast(urlPath, "/");
            return ResponseEntity.ok(miniDataStore.get(scmBaseUrl));
        };
    }
}
