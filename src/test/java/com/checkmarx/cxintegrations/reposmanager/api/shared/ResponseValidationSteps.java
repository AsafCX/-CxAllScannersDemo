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
public class ResponseValidationSteps {
    private final ApiTestState testState;

    @Then("response status is {int}")
    public void responseStatusIs(int expectedStatus) {
        Assert.assertNotNull("Expected the response to be non-null at this point.", testState.getLastResponse());
        Assert.assertEquals("Unexpected response status.", expectedStatus, testState.getLastResponse().getStatusCodeValue());
    }

    @And("response contains the {word} field set to {word}")
    public void responseContainsTheFieldSetTo(String fieldName, String expectedFieldValue) {
        JsonNode responseBody = getBody(testState.getLastResponse());

        String message = String.format("Unexpected value for the '%s' response field.", fieldName);
        JsonNode fieldToCheck = responseBody.get(fieldName);
        Assert.assertNotNull(String.format("The %s response field is missing.", fieldName), fieldToCheck);

        Assert.assertEquals(message, expectedFieldValue, fieldToCheck.textValue());

        testState.getExpectedFieldNames().add(fieldName);
    }

    @And("response does not have any other fields")
    public void responseDoesNotHaveOtherFields() {
        JsonNode responseBody = getBody(testState.getLastResponse());
        Set<String> actualFields = getFieldsFrom(responseBody);

        Set<String> expectedFields = new HashSet<>(testState.getExpectedFieldNames());

        Assert.assertEquals("Actual response fields don't match the expected ones.", expectedFields, actualFields);
    }

    @And("response contains the following fields, all non-empty:")
    public void responseContainsNonEmptyAndFields(List<String> fields) {
        JsonNode responseBody = getBody(testState.getLastResponse());
        fields.forEach(fieldName -> checkIfFieldIsNotEmpty(fieldName, responseBody));
    }

    private Set<String> getFieldsFrom(JsonNode responseBody) {
        Spliterator<String> spliterator = Spliterators.spliteratorUnknownSize(responseBody.fieldNames(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false)
                .collect(Collectors.toSet());
    }

    private static JsonNode getBody(ResponseEntity<JsonNode> response) {
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
}
