package com.checkmarx.cxintegrations.reposmanager.api.webhook;

import com.checkmarx.cxintegrations.reposmanager.WebApiRequestSender;
import com.checkmarx.cxintegrations.reposmanager.dto.ApiTestState;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.github.WebhookGithubDto;
import com.checkmarx.dto.gitlab.AccessTokenGitlabDto;
import com.checkmarx.dto.gitlab.WebhookGitLabDto;
import com.checkmarx.service.DataStoreService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * The following happens here:
 *      Test invokes ReposManager webhook APIs via HTTP calls.
 *      SCM APIs are mocked but mimic response statuses of actual SCMs.
 *      DataStore is mocked. The mock is used to return dummy SCM access tokens.
 */
@Slf4j
@RequiredArgsConstructor
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RepoWebhookApiSteps {
    private static final String ORG_ID = "myGreatOrg";
    private static final String REPO_ID = "myAwesomeRepo";
    private static final String CX_INTEGRATIONS_WEBHOOK_ID = "cxint-webhook-id";
    private static final String THIRD_PARTY_WEBHOOK_ID = "third-party-webhook-id";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @LocalServerPort
    private int apiPort;

    @MockBean
    private final RestTemplate restTemplateMock;

    @MockBean
    private final DataStoreService dataStoreServiceMock;

    private final ApiTestState testState;
    private final WebApiRequestSender requestSender;

    private String currentScmId;

    private final Set<String> existingWebhookIds = new HashSet<>();

    @Before
    public void beforeEachScenario() {
        testState.clear();
        existingWebhookIds.clear();
        initMocks();
    }

    @Given("CxIntegrations webhook {string} in a {word} repo")
    public void cxintegrationsWebhookExistsOrNotInAScmRepo(String existsOrNot, String scmId) {
        if (shouldExist(existsOrNot)) {
            existingWebhookIds.add(CX_INTEGRATIONS_WEBHOOK_ID);
        } else {
            existingWebhookIds.remove(CX_INTEGRATIONS_WEBHOOK_ID);
        }
        currentScmId = scmId;
    }

    @When("API client calls the `create webhook` API for the repo")
    public void apiClientCallsTheCreateWebhookAPIForTheRepo() {
        testState.prepareForRequestSending();

        ResponseEntity<String> response = requestSender.genericSend(
                "{scmId}/orgs/{orgId}/repos/{repoId}/webhooks",
                apiPort, HttpMethod.POST, null, currentScmId, ORG_ID, REPO_ID);

        testState.setLastResponse(response);
    }

    @When("API client calls the `delete webhook` API for the repo")
    public void apiClientCallsTheDeleteWebhookAPIForTheRepo() {
        testState.prepareForRequestSending();

        ResponseEntity<String> response = requestSender.genericSend(
                "{scmId}/orgs/{orgId}/repos/{repoId}/webhooks/{webhookId}",
                apiPort, HttpMethod.DELETE, null, currentScmId, ORG_ID, REPO_ID, CX_INTEGRATIONS_WEBHOOK_ID);

        testState.setLastResponse(response);
    }

    @And("the response contains a non-empty webhook ID")
    public void theResponseContainsANonEmptyWebhookID() {
        String body = testState.getLastResponse().getBody();
        Assert.assertTrue("Webhook ID is empty.", StringUtils.isNotEmpty(body));
    }

    @And("CxIntegrations webhook is created in the repo")
    public void cxintegrationsWebhookIsCreatedInTheRepo() {
        verifyWebhookExistence(CX_INTEGRATIONS_WEBHOOK_ID, true);
    }

    @And("a third-party webhook exists in the repo")
    public void aThirdPartyWebhookExistsInTheRepo() {
        existingWebhookIds.add(THIRD_PARTY_WEBHOOK_ID);
    }

    @And("CxIntegrations webhook is deleted from the repo")
    public void cxintegrationsWebhookIsDeletedFromTheRepo() {
        verifyWebhookExistence(CX_INTEGRATIONS_WEBHOOK_ID, false);
    }

    @But("the third-party webhook still exists in the repo")
    public void theThirdPartyWebhookStillExistsInTheRepo() {
        verifyWebhookExistence(THIRD_PARTY_WEBHOOK_ID, true);
    }

    private void initMocks() {
        final String IN_GITHUB = "api.github.com";
        final String IN_GITLAB = "gitlab.com/api";

        log.info("Initializing mocks.");

        when(reposManagerAsksForScmToken())
                .thenAnswer(withFakeToken());

        when(reposManagerCreatesWebhook(IN_GITHUB))
                .thenAnswer(withFakeGitHubResponse());

        when(reposManagerCreatesWebhook(IN_GITLAB))
                .thenAnswer(withFakeGitLabResponse());

        when(reposManagerDeletesWebhook(IN_GITHUB))
                .thenAnswer(withFakeDeletionResponse());

        when(reposManagerDeletesWebhook(IN_GITLAB))
                .thenAnswer(withFakeDeletionResponse());
    }

    private ResponseEntity<?> reposManagerCreatesWebhook(String scmApiBaseUrl) {
        // https://api.github.com/repos/orgId/repoId/hooks
        // https://gitlab.com/api/v4/projects/projectId/hooks?url=...
        return restTemplateMock.exchange(
                argThat(refersToWebhook(scmApiBaseUrl)),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(Class.class));
    }

    private ResponseEntity<?> reposManagerDeletesWebhook(String scmApiBaseUrl) {
        // https://api.github.com/repos/orgId/repoId/hooks/webhookId
        // https://gitlab.com/api/v4/projects/repoId/hooks/webhookId
        return restTemplateMock.exchange(
                argThat(refersToWebhook(scmApiBaseUrl)),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                any(Class.class));
    }

    private Answer<?> withFakeDeletionResponse() {
        return invocation -> {
            log.info("Intercepted webhook deletion request: {}", invocation);
            String webhookId = extractWebhookId(invocation);
            HttpStatus status = (existingWebhookIds.contains(webhookId) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND);
            existingWebhookIds.remove(webhookId);
            return new ResponseEntity<>(status);
        };
    }

    private String extractWebhookId(InvocationOnMock invocation) {
        String scmApiUrl = invocation.getArgument(0);
        String webhookId = StringUtils.substringAfterLast(scmApiUrl, "/");
        log.info("Extracted webhook ID from webhook deletion request: {}", webhookId);
        return webhookId;
    }

    private static ArgumentMatcher<String> refersToWebhook(String scmApiBaseUrl) {
        return (String arg) -> arg != null && arg.contains(scmApiBaseUrl) && arg.contains("/hooks");
    }

    private static Answer<ScmAccessTokenDto> withFakeToken() {
        return invocation -> {
            log.info("Creating a fake SCM access token.");
            String token;
            String scmUrl = invocation.getArgument(0);
            if (scmUrl.contains("gitlab")) {
                token = getGitlabSpecificTokenStub();
            } else {
                token = "anyValue";
            }
            return ScmAccessTokenDto.builder()
                    .accessToken(token)
                    .build();
        };
    }

    private static String getGitlabSpecificTokenStub() throws JsonProcessingException {
        // GitLabService expects to get a token as a JSON string in a particular format.
        AccessTokenGitlabDto internalObject = AccessTokenGitlabDto.builder()
                .accessToken("anyValue")
                .build();

        String result = objectMapper.writeValueAsString(internalObject);
        log.info("Created a GitLab-specific fake access token: {}", result);

        return result;
    }

    private ScmAccessTokenDto reposManagerAsksForScmToken() {
        return dataStoreServiceMock.getSCMOrgToken(anyString(), any());
    }

    private Answer<Object> withFakeGitHubResponse() {
        logFakeCreationResponse(WebhookGithubDto.class);
        return getGenericScmAnswer(WebhookGithubDto.builder()
                .id(CX_INTEGRATIONS_WEBHOOK_ID)
                .build());
    }

    private Answer<Object> withFakeGitLabResponse() {
        logFakeCreationResponse(WebhookGitLabDto.class);
        return getGenericScmAnswer(WebhookGitLabDto.builder()
                .id(CX_INTEGRATIONS_WEBHOOK_ID)
                .build());
    }

    private static void logFakeCreationResponse(Class<?> responseClass) {
        log.info("Returning a fake webhook creation response: {}", responseClass.getSimpleName());
    }

    private <T> Answer<Object> getGenericScmAnswer(T body) {
        return invocation -> {
            log.info("Intercepted webhook creation request: {}", invocation);
            existingWebhookIds.add(CX_INTEGRATIONS_WEBHOOK_ID);
            return new ResponseEntity<>(body, HttpStatus.CREATED);
        };
    }

    private void verifyWebhookExistence(String webhookId, boolean shouldExist) {
        String template;
        if (shouldExist) {
            template = "The %s webhook should exist in the repo, but it doesn't.";
        } else {
            template = "The %s webhook should not exist in the repo, but it does.";
        }
        Assert.assertEquals(String.format(template, webhookId), shouldExist, existingWebhookIds.contains(webhookId));
    }

    private static boolean shouldExist(String existsOrNot) {
        return existsOrNot.equals("exists");
    }
}
