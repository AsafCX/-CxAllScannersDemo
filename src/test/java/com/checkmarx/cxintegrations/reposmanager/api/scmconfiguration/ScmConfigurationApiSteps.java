package com.checkmarx.cxintegrations.reposmanager.api.scmconfiguration;

import com.checkmarx.dto.datastore.ScmDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Slf4j
@RequiredArgsConstructor
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ScmConfigurationApiSteps {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int apiPort;

    @MockBean
    private final RestTemplate restTemplateMock;

    private final Map<String, ScmDto> miniDataStore = new HashMap<>();

    private ResponseEntity<JsonNode> response;

    @Before
    public void useFakeDataStoreResponses() {
        when(restTemplateMock.exchange(contains("/scms/"), eq(HttpMethod.GET), any(), any(Class.class)))
                .thenAnswer(usingMiniDataStore());
    }

    @Given("data store contains {word} and {word} for the {word} SCM")
    public void dataStoreContains(String clientId, String scope, String scmId) {
        String scmBaseUrl = (scmId.equals("github") ? "github.com" : "gitlab.com");
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
        RestTemplate client = new RestTemplate();
        try {
            response = client.getForEntity("http://localhost:{port}/{scmId}/config", JsonNode.class, apiPort, scmId);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Caught a {} exception with the message: {}", e.getClass().getName(), e.getMessage());
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsByteArray());
            response = new ResponseEntity<>(body, e.getStatusCode());
        }
    }

    @Then("response status is {int}")
    public void responseStatusIs(int expectedStatus) {
        Assert.assertNotNull("Expected the response to be non-null at this point.", response);
        Assert.assertEquals("Unexpected response status.", expectedStatus, response.getStatusCodeValue());
    }

    @And("response contains the {word} field set to {word}")
    public void responseContainsTheFieldSetTo(String fieldName, String expectedFieldValue) {
        JsonNode responseBody = getBodyIfPresent(response);

        String message = String.format("Unexpected value for the '%s' response field.", fieldName);
        JsonNode fieldToCheck = responseBody.get(fieldName);
        Assert.assertNotNull(String.format("The %s response field is missing.", fieldName), fieldToCheck);

        Assert.assertEquals(message, expectedFieldValue, fieldToCheck.textValue());
    }

    @And("response does not have other fields rather than")
    public void responseDoesNotHaveOtherFields(List<String> expectedFieldNames) {
        JsonNode responseBody = getBodyIfPresent(response);

        Spliterator<String> spliterator = Spliterators.spliteratorUnknownSize(responseBody.fieldNames(), Spliterator.ORDERED);
        Set<String> actualFieldSet = StreamSupport.stream(spliterator, false)
                .collect(Collectors.toSet());

        HashSet<String> expectedFieldSet = new HashSet<>(expectedFieldNames);
        Assert.assertEquals("Actual response fields don't match the expected ones.", expectedFieldSet, actualFieldSet);
    }

    @Given("data store does not contain the {word} SCM")
    public void dataStoreDoesNotContainTheSCM(String sdmId) {
        miniDataStore.clear();
    }

    @And("response contains the following fields, all non-empty:")
    public void responseContainsNonEmptyAndFields(List<String> fields) {
        JsonNode responseBody = getBodyIfPresent(response);
        fields.forEach(fieldName -> checkIfFieldIsNotEmpty(fieldName, responseBody));
    }

    private static JsonNode getBodyIfPresent(ResponseEntity<JsonNode> response) {
        JsonNode responseBody = response.getBody();
        Assert.assertNotNull("Response body must not be null.", responseBody);
        return responseBody;
    }

    private void checkIfFieldIsNotEmpty(String field, JsonNode responseBody) {
        JsonNode responseField = responseBody.get(field);
        String fieldIsMissing = String.format("The '%s' field is missing in the response.", field);
        Assert.assertNotNull(fieldIsMissing, responseField);

        String actualFieldValue = responseField.textValue();
        String valueIsEmpty = String.format("The '%s' response field is empty.", field);
        Assert.assertTrue(valueIsEmpty, StringUtils.isNotEmpty(actualFieldValue));
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
