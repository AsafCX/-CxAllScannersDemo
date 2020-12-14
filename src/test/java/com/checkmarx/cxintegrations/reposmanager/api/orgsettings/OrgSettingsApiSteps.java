package com.checkmarx.cxintegrations.reposmanager.api.orgsettings;

import com.checkmarx.cxintegrations.reposmanager.WebApiRequestSender;
import com.checkmarx.cxintegrations.reposmanager.dto.ApiTestState;
import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.checkmarx.service.GitHubService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

/**
 * Here we call the actual ReposManager API, but intercept and fake the requests that ReposManager sends to DataStore.
 * An in-memory DataStore replacement is used.
 */
@Slf4j
@RequiredArgsConstructor
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrgSettingsApiSteps {
    private MiniOrgStore miniOrgStore;

    @LocalServerPort
    private int apiPort;

    @MockBean
    private final RestTemplate restTemplateMock;

    private final ApiTestState testState;
    private final WebApiRequestSender requestSender;
    private final GitHubService gitHubService;

    @Before
    public void useFakeDataStoreResponses() {
        testState.clear();
        miniOrgStore = new MiniOrgStore();

        RequestInterceptor usingFakeResponses = new RequestInterceptor(miniOrgStore);
        when(reposManagerCallsDataStoreApi()).thenAnswer(usingFakeResponses);
    }

    @Given("data store contains an organization with the {word} ID, belonging to the github SCM")
    public void dataStoreContainsOrg(String orgId) {
        miniOrgStore.addOrg(orgId, gitHubService.getBaseDbKey());
    }

    @Given("the {word} organization has {word} CxGo secret and {word} team")
    public void orgHasSecretAndTeam(String orgId, String cxgoSecret, String team) {
        OrgPropertiesDto org = miniOrgStore.getOrganization(orgId);
        validateOrgIsPresent(orgId, org);

        org.setCxGoToken(cxgoSecret);
        org.setCxTeam(team);
    }

    @Then("the {word} organization now has {word} CxGo secret and {word} team")
    public void orgNowHasSecretAndTeam(String orgId, String cxgoSecret, String team) {
        OrgPropertiesDto org = miniOrgStore.getOrganization(orgId);

        validateOrgIsPresent(orgId, org);
        Assert.assertEquals("Unexpected CxGo secret.", cxgoSecret, org.getCxGoToken());
        Assert.assertEquals("Unexpected team.", team, org.getCxTeam());
    }

    @When("API client creates a request with cxgoSecret field set to {word} and team field set to {word}")
    public void apiClientCreatesARequest(String cxgoSecret, String team) {
        testState.setRequestForSending(OrgSettingsWebDto.builder()
                .cxgoSecret(cxgoSecret)
                .team(team)
                .build());
    }

    @When("API client calls the 'get organization settings' API for the {word} organization of {word} SCM")
    public void apiClientCallsGetOrgSettingsApi(String orgId, String scmId) {
        callReposManagerApi(null, orgId, scmId);
    }

    @When("API client calls the 'save organization settings' API for the {word} organization of {word} SCM, using the request above")
    public void apiClientCallsSaveOrgSettingsApi(String orgId, String scmId) {
        callReposManagerApi(new HttpEntity<>(testState.getRequestForSending()), orgId, scmId);
    }

    @When("API client calls the 'save organization settings' API for the {word} SCM")
    public void apiClientCallsSaveOrgSettingsApiForScm(String scmId) {
        apiClientCallsGetOrgSettingsApi("any-org-id", scmId);
    }

    @Given("{word} is the only organization belonging to the {word} SCM")
    public void isTheOnlyOrg(String orgId, String scmId) {
        miniOrgStore.clearScmOrgs(scmId);
        miniOrgStore.addOrg(orgId, scmId);
    }

    @Given("data store does not contain any organizations for the {word} SCM")
    public void noOrgsForScm(String scmId) {
        miniOrgStore.clearScmOrgs(scmId);
    }

    @Given("myAmazingScm SCM does not exist in data store")
    public void scmDoesNotExist() {
        // Nothing to do here: there is no place where this SCM is added.
    }

    @Then("data store now contains an organization with the {word} ID, belonging to the github SCM")
    public void dataStoreNowContainsOrg(String orgId) {
        OrgPropertiesDto org = miniOrgStore.findOrg(orgId, gitHubService.getBaseDbKey());
        validateOrgIsPresent(orgId, org);
    }

    private void callReposManagerApi(HttpEntity<?> requestBody, String orgId, String scmId) {
        testState.prepareForRequestSending();
        HttpMethod method = (requestBody == null ? HttpMethod.GET : HttpMethod.PUT);
        ResponseEntity<String> response = requestSender.genericSend("{scmId}/orgs/{orgId}/settings",
                apiPort,
                method,
                requestBody,
                scmId,
                orgId);
        testState.setLastResponse(response);
    }

    private ResponseEntity<?> reposManagerCallsDataStoreApi() {
        return restTemplateMock.exchange(contains("/orgs/properties"),
                any(HttpMethod.class),
                any(),
                any(Class.class));
    }

    private static void validateOrgIsPresent(String orgId, OrgPropertiesDto org) {
        Assert.assertNotNull(String.format("The '%s' organization is missing in data store!", orgId), org);
    }
}
