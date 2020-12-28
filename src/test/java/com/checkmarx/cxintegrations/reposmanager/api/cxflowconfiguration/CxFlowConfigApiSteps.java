package com.checkmarx.cxintegrations.reposmanager.api.cxflowconfiguration;

import com.checkmarx.cxintegrations.reposmanager.*;
import com.checkmarx.cxintegrations.reposmanager.api.orgsettings.MiniOrgStore;
import com.checkmarx.cxintegrations.reposmanager.dto.ApiTestState;
import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.service.GitHubService;
import com.checkmarx.service.GitLabService;
import com.checkmarx.service.ScmService;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@CucumberContextConfiguration
@RequiredArgsConstructor
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CxFlowConfigApiSteps {
    private static final String SAMPLE_ORGANIZATION = "arbitrary-org-id";

    private MiniOrgStore miniStore;

    @LocalServerPort
    private int apiPort;

    @MockBean
    private final RestTemplate restTemplateMock;

    private final ApiTestState testState;
    private final WebApiRequestSender requestSender;
    private final GitHubService gitHubService;
    private final GitLabService gitLabService;
    private final QueryStringInterceptor queryStringInterceptor;
    private final List<ScmAccessTokenDto> scmAccessTokens = new ArrayList<>();
    private final FakeAccessTokenGenerator fakeAccessTokenGenerator;

    private boolean considerScmAccessTokenValid;

    @Before
    public void useFakeDataStoreResponses() {
        testState.clear();
        miniStore = new MiniOrgStore();
        scmAccessTokens.clear();
        considerScmAccessTokenValid = true;

        OrgRequestInterceptor usingFakeResponses = new OrgRequestInterceptor(miniStore, queryStringInterceptor);

        when(reposManagerAsksForOrgSettings()).thenAnswer(usingFakeResponses);
        when(reposManagerAsksForToken()).thenAnswer(withFakeToken());
        when(reposManagerValidatesGitHubToken()).thenAnswer(withFakeValidationResult());
    }

    @Given("data store contains {string}, {string} and {string} for a specific organization in {word}")
    public void dataStoreContains(String team, String cxgoSecret, String scmAccessToken, String scmId) throws JsonProcessingException {
        String scmUrl = toScmUrl(scmId);

        OrgPropertiesDto orgProperties = OrgPropertiesDto.builder()
                .orgIdentity(SAMPLE_ORGANIZATION)
                .scmUrl(scmUrl)
                .cxTeam(team)
                .cxGoToken(cxgoSecret)
                .build();

        ScmAccessTokenDto tokenDto = fakeAccessTokenGenerator.generate(scmUrl, scmAccessToken);
        tokenDto.setOrgIdentity(SAMPLE_ORGANIZATION);

        miniStore.updateOrCreateOrg(orgProperties);
        scmAccessTokens.add(tokenDto);
    }

    @And("the SCM access token is valid for {word} API calls")
    public void theSCMAccessTokenIsValidForSCMAPICalls(String scmId) {
        considerScmAccessTokenValid = true;
    }

    @And("GitHub access token is invalid")
    public void githubAccessTokenIsInvalid() {
        considerScmAccessTokenValid = false;
    }

    @When("CxFlow calls ReposManager API to get this organization config in {word} SCM")
    public void cxflowCallsReposManager(String scmId) {
        callCxFlowConfigurationApi(scmId, SAMPLE_ORGANIZATION);
    }

    @When("CxFlow calls ReposManager API to get configuration for the {word} organization of {word} SCM")
    public void cxflowCallsReposManagerForScm(String orgId, String scmId) {
        callCxFlowConfigurationApi(scmId, orgId);
    }

    @Given("{word} is the only organization belonging to the {word} SCM")
    public void isTheOnlyOrg(String orgId, String scmId) {
        miniStore.clearScmOrgs(scmId);
        miniStore.addOrg(orgId, scmId);
    }

    private String toScmUrl(String scmId) {
        ScmService targetService = (scmId.equals("github") ? gitHubService : gitLabService);
        return targetService.getBaseDbKey();
    }

    private void callCxFlowConfigurationApi(String scmId, String orgId) {
        testState.prepareForRequestSending();
        ResponseEntity<String> response = requestSender.get("{scmId}/orgs/{orgId}/tenantConfig",
                apiPort, scmId, orgId);
        testState.setLastResponse(response);
    }

    private ResponseEntity<?> reposManagerValidatesGitHubToken() {
        return matchGetRequest("api.github.com/user");
    }

    private ResponseEntity<?> reposManagerAsksForOrgSettings() {
        return matchGetRequest("/orgs/properties");
    }

    private ResponseEntity<?> reposManagerAsksForToken() {
        return matchGetRequest("/tokens");
    }

    private ResponseEntity<?> matchGetRequest(String urlSubstring) {
        return restTemplateMock.exchange(contains(urlSubstring),
                eq(HttpMethod.GET),
                any(),
                any(Class.class));
    }

    private Answer<?> withFakeValidationResult() {
        return invocation -> {
            if (considerScmAccessTokenValid) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED,
                        "The fake access token is considered to be invalid.");
            }
        };
    }

    private Answer<?> withFakeToken() {
        return (InvocationOnMock invocation) -> {
            // GET .../tokens?scmUrl=%s&orgIdentity=%s
            String scmUrl = queryStringInterceptor.getParamValue(invocation, "scmUrl");
            String orgId = queryStringInterceptor.getParamValue(invocation, "orgIdentity");
            return ResponseEntity.ok(scmAccessTokens.stream()
                    .filter(token ->
                            token.getOrgIdentity().equals(orgId) &&
                                    token.getScmUrl().equals(scmUrl))
                    .findFirst()
                    .orElseThrow(() -> new HttpClientErrorException(HttpStatus.NOT_FOUND)));
        };
    }
}
