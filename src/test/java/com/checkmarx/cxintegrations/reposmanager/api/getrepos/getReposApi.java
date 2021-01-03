package com.checkmarx.cxintegrations.reposmanager.api.getrepos;

import com.checkmarx.cxintegrations.reposmanager.FakeAccessTokenGenerator;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.github.GithubEvent;
import com.checkmarx.dto.github.RepoGithubDto;
import com.checkmarx.dto.github.WebhookGithubDto;
import com.checkmarx.dto.gitlab.RepoGitlabDto;
import com.checkmarx.dto.gitlab.WebhookGitLabDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.service.*;
import com.checkmarx.utils.RestWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@Slf4j
@RequiredArgsConstructor
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class getReposApi {

    private static final String GITHUB = "github";
    private static final String GITLAB = "gitlab";
    private static final String HOOKS = "hooks";
    private static final String cxFlowUrl = "dummyUrl";
    private FakeAccessTokenGenerator fakeAccessTokenGenerator = new FakeAccessTokenGenerator();
    private static final String ORG_ID = "myOrganization";
    private final RestWrapper restWrapper = mock(RestWrapper.class);
    
    private DataService dataService = mock(DataService.class);
    private GitHubService gitHubService ;
    private GitLabService gitLabService ;
    private final AccessTokenService tokenService;
    private String scmType;
    private int numberRepos;
    private int numberHooks;
    private int numberActiveHooks;
    private int nHooksWithEvents;
    private List<RepoWebDto> apiResult;
    private int hooksWithUrl;
    private ResponseEntity<RepoGithubDto[]> githubReposMockResult;
    private List<WebhookGithubDto> githubWebhooksMockResult;
    private ResponseEntity<RepoGitlabDto[]> gitlabReposMockResult;
    private List<WebhookGitLabDto> gitlabWebhooksMockResult;


    private void initMocks() {
        gitHubService = spy(new GitHubService(restWrapper, dataService, tokenService));
        gitLabService = spy(new GitLabService(restWrapper, dataService, tokenService));
        doReturn(cxFlowUrl).when(gitHubService).getCxFlowUrl();
        doReturn(cxFlowUrl).when(gitLabService).getCxFlowUrl();

        HttpRequestInterceptor httpRequestInterceptor = new HttpRequestInterceptor();
        when(restWrapper.sendBearerAuthRequest(any(), any(), any(), any(), any(), any())).thenAnswer(httpRequestInterceptor);
        doNothing().when(dataService).updateScmOrgRepo(any());
        try {
            ScmAccessTokenDto accessTokenDto = fakeAccessTokenGenerator.generate(scmType, "fake-token");
            when(dataService.getSCMOrgToken(any(), any())).thenReturn(accessTokenDto);
        } catch (JsonProcessingException e) {
            Assert.fail(e.getMessage());
        }

    }


    @And("There are {int} webhooks defined on the repositories")
    public void thereAreN_hooksWebhooksDefinedOnTheRepositories(int n_hooks) {
        this.numberHooks = n_hooks;
    }

    @And("number of active hooks is {string}")
    public void numberOfActiveHooksIsN_active_hooks(String n_active_hooks) {
        if (n_active_hooks.equals("irrelevant")) {
            this.numberActiveHooks = numberRepos;

        } else {
            this.numberActiveHooks = Integer.parseInt(n_active_hooks);
        }
    }

    @And("number of hooks with valid events will be {int}")
    public void numberOfHooksWithValidEventsWillBeN_hooks_with_valid_events(int hooks_with_valid_events) {
        this.nHooksWithEvents = hooks_with_valid_events;
    }

    @And("number of returned repositories will be {int}")
    public void numberOfReturnedRepositoriesWillBeN_repos(Integer n_repos) {
        numberRepos = n_repos;
    }


    @When("API get repositories is called with scm {string}")
    public void apiGetRepositoriesIsCalledWithScm(String scm) {
        scmType = scm;
        numberRepos = 0;
        numberHooks = 0;
        numberActiveHooks = 0;
        nHooksWithEvents = 0;
        initMocks();
    }

    @And("number of hooks with CxFlow url is {}")
    public void numberOfHooksWithCxFlowUrlIsN_hooks_with_url(Integer n_hooks_with_url) {
        this.hooksWithUrl = n_hooks_with_url;
    }

    private class HttpRequestInterceptor implements Answer<ResponseEntity> {

        @Override
        public ResponseEntity answer(InvocationOnMock invocation) {
            String url = invocation.getArgument(0);
            log.info("HttpRequestInterceptor url: " + url);
            if (url.contains(GITHUB) && !url.contains(HOOKS)) {
                return githubReposMockResult;
            }
            if (url.contains(GITHUB) && url.contains(HOOKS)) {
                return getWebhookGithubResponse();
            }
            if (url.contains(GITLAB) && !url.contains(HOOKS)) {
                return gitlabReposMockResult;
            }
            if (url.contains(GITLAB) && url.contains(HOOKS)) {
                return getWebhookGitlabResponse();
            }
            return null;
        }
    }

    private ResponseEntity getWebhookGitlabResponse() {
        WebhookGitLabDto[] gitlabWebhooksMockResultArr;
        if (gitlabWebhooksMockResult.size() > 0) {
            WebhookGitLabDto webhook = gitlabWebhooksMockResult.get(0);
            gitlabWebhooksMockResult.remove(0);
            gitlabWebhooksMockResultArr = new WebhookGitLabDto[1];
            gitlabWebhooksMockResultArr[0] = webhook;
        } else {
            gitlabWebhooksMockResultArr = new WebhookGitLabDto[0];
        }
        return new ResponseEntity<>(gitlabWebhooksMockResultArr, HttpStatus.OK);
    }

    private ResponseEntity getWebhookGithubResponse() {
        WebhookGithubDto[] githubWebhooksMockResultArr;
        if (githubWebhooksMockResult.size() > 0) {
            WebhookGithubDto webhook = githubWebhooksMockResult.get(0);
            githubWebhooksMockResult.remove(0);
            githubWebhooksMockResultArr = new WebhookGithubDto[1];
            githubWebhooksMockResultArr[0] = webhook;
        } else {
            githubWebhooksMockResultArr = new WebhookGithubDto[0];
        }
        return new ResponseEntity<>(githubWebhooksMockResultArr, HttpStatus.OK);
    }

    private List<WebhookGithubDto> prepareGithubWebhooksResult() {
        List<WebhookGithubDto> webhookGithubDtos = new LinkedList<>();
        for (int i = 0; i < numberHooks; i++) {
            WebhookGithubDto repoGithubDto = new WebhookGithubDto();
            repoGithubDto.setId("" + i);
            repoGithubDto.setName("repo " + i);

            if (numberActiveHooks > 0) {
                repoGithubDto.setActive(true);
                numberActiveHooks--;
            } else {
                repoGithubDto.setActive(false);
            }

            if (nHooksWithEvents > 0) {
                repoGithubDto.setEvents(GithubEvent.getAllEventsList());
                nHooksWithEvents--;
            }

            if (hooksWithUrl > 0) {
                repoGithubDto.setUrl(cxFlowUrl);
                hooksWithUrl--;
            } else {
                repoGithubDto.setUrl("invalid");
            }

            webhookGithubDtos.add(repoGithubDto);
        }
        return webhookGithubDtos;
    }

    private List<WebhookGitLabDto> prepareGitlabWebhooksResult() {
        List<WebhookGitLabDto> webhookGitlabDtos = new LinkedList<>();

        for (int i = 0; i < numberHooks; i++) {
            WebhookGitLabDto webhookGitLabDto = new WebhookGitLabDto();
            webhookGitLabDto.setId("" + i);

            if (nHooksWithEvents == 0) {
                webhookGitLabDto.setPushEvents(false);
                webhookGitLabDto.setMergeRequestsEvents(false);
            }
            if (nHooksWithEvents > 0) {
                webhookGitLabDto.setPushEvents(true);
                webhookGitLabDto.setMergeRequestsEvents(true);
                nHooksWithEvents--;
            }
            if (hooksWithUrl > 0) {
                webhookGitLabDto.setUrl(cxFlowUrl);
                hooksWithUrl--;
            } else {
                webhookGitLabDto.setUrl("invalid");
            }

            webhookGitlabDtos.add(webhookGitLabDto);
        }
        return webhookGitlabDtos;
    }

    private ResponseEntity<RepoGithubDto[]> prepareGithubReposResult() {
        RepoGithubDto[] listGithub = new RepoGithubDto[numberRepos];
        for (int i = 0; i < numberRepos; i++) {
            RepoGithubDto repoGithubDto = new RepoGithubDto();
            repoGithubDto.setId("" + i);
            repoGithubDto.setName("repo " + i);
            listGithub[i] = repoGithubDto;
        }
        return new ResponseEntity<>(listGithub, HttpStatus.OK);
    }

    private ResponseEntity<RepoGitlabDto[]> prepareGitlabReposResult() {
        RepoGitlabDto[] repoGitlabDtos = new RepoGitlabDto[numberRepos];
        for (int i = 0; i < numberRepos; i++) {
            RepoGitlabDto repoGithubDto = new RepoGitlabDto();
            repoGithubDto.setId("" + i);
            repoGithubDto.setName("repo " + i);
            repoGitlabDtos[i] = repoGithubDto;
        }
        return new ResponseEntity<>(repoGitlabDtos, HttpStatus.OK);
    }

    @Then("repositories list returned by CxIntegration will be {int}")
    public void repositoriesListReturnedByCxIntegrationWillBeN_repos(int numExpectedRepos) {

        if (scmType.equals(GITHUB)) {
            githubReposMockResult = prepareGithubReposResult();
            githubWebhooksMockResult = prepareGithubWebhooksResult();
            apiResult = gitHubService.getScmOrgRepos(ORG_ID);
        }
        if (scmType.equals(GITLAB)) {
            gitlabReposMockResult = prepareGitlabReposResult();
            gitlabWebhooksMockResult = prepareGitlabWebhooksResult();
            apiResult = gitLabService.getScmOrgRepos(ORG_ID);
        }
        log.info(scmType + ": Validating get repos count");
        Assert.assertEquals(scmType + ": Validating get repos count: "+ numExpectedRepos + " vs " + apiResult.size(), numExpectedRepos, apiResult.size());
    }

    @And("number of effective hooks will be {int}")
    public void numberOfEffectiveHooksWillBeN_effective_hooks(Integer n_effective_hooks) {
        int countWebhooks = 0;
        for (RepoWebDto currResult : apiResult) {
            if (currResult.isWebhookEnabled()) {
                countWebhooks++;
            }
        }
        log.info(scmType + ": Validating effective hooks count");
        Assert.assertEquals(scmType + ": Validating effective hooks count: "+ n_effective_hooks + " vs " + countWebhooks, n_effective_hooks.intValue(), countWebhooks);
    }


}
