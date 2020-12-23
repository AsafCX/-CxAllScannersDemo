package com.checkmarx.cxintegrations.reposmanager.api.shared;

import com.checkmarx.cxintegrations.reposmanager.dto.ApiTestState;
import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class ResponseValidationSharedSteps {
    private final ApiTestState testState;

    @Then("response status is {int}")
    public void responseStatusIs(int expectedStatus) {
        ResponseEntity<String> response = testState.getLastResponse();

        Assert.assertNotNull("Expected the response to be non-null at this point.", response);
        Assert.assertEquals("Unexpected response status.", expectedStatus, response.getStatusCodeValue());
    }

    @And("response contains the {word} field set to {word}")
    public void responseContainsTheFieldSetTo(String fieldName, String expectedFieldValue) {
        JsonNode responseBody = getBody(testState);

        JsonNode fieldToCheck = getNonNullField(responseBody, fieldName);

        String message = String.format("Unexpected value for the '%s' response field.", fieldName);
        Assert.assertEquals(message, expectedFieldValue, fieldToCheck.textValue());

        testState.getExpectedFieldNames().add(fieldName);
    }

    @And("response has the {word} field containing the text: {string}")
    public void responseHasTheMessageFieldContainingTheText(String fieldName, String expectedText) {
        JsonNode responseBody = getBody(testState);
        JsonNode fieldToCheck = getNonNullField(responseBody, fieldName);
        Assert.assertTrue(String.format("The '%s' response field doesn't contain the expected value: '%s'\n%s", fieldName, expectedText, responseBody), StringUtils.containsIgnoreCase(fieldToCheck.textValue(), expectedText));
    }

    @And("response does not have any other fields")
    public void responseDoesNotHaveOtherFields() {
        JsonNode responseBody = getBody(testState);
        Set<String> actualFields = getFieldsFrom(responseBody);

        Set<String> expectedFields = new HashSet<>(testState.getExpectedFieldNames());

        Assert.assertEquals("Actual response fields don't match the expected ones.", expectedFields, actualFields);
    }

    @And("response contains a standard error message")
    public void responseContainsNonEmptyAndFields() {
        JsonNode responseBody = getBody(testState);
        checkIfFieldIsNotEmpty("message", responseBody);
        checkIfFieldIsNotEmpty("localDateTime", responseBody);
    }

    private Set<String> getFieldsFrom(JsonNode responseBody) {
        Spliterator<String> spliterator = Spliterators.spliteratorUnknownSize(responseBody.fieldNames(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false)
                .collect(Collectors.toSet());
    }

    private static JsonNode getBody(ApiTestState testState) {
        ResponseEntity<JsonNode> response = testState.getLastResponseAsJson();
        Assert.assertNotNull("JSON response entity must not be null", response);

        JsonNode result = response.getBody();
        Assert.assertNotNull("Response body must not be null.", result);
        return result;
    }

    private static JsonNode getNonNullField(JsonNode json, String fieldName) {
        JsonNode result = json.get(fieldName);
        Assert.assertNotNull(String.format("The '%s' response field is null.", fieldName), fieldName);
        return result;
    }

    private static void checkIfFieldIsNotEmpty(String field, JsonNode responseBody) {
        JsonNode responseField = getNonNullField(responseBody, field);

        String actualFieldValue = responseField.textValue();
        String valueIsEmpty = String.format("The '%s' response field is empty.", field);
        Assert.assertTrue(valueIsEmpty, StringUtils.isNotEmpty(actualFieldValue));
    }
}
