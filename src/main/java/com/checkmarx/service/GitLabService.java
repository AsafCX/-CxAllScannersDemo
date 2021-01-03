package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.BaseDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.gitlab.*;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service("gitlab")
public class GitLabService extends AbstractScmService implements ScmService  {

    private static final String URL_AUTH_TOKEN = "https://gitlab.com/oauth/token";

    private static final String URL_REFRESH_TOKEN = URL_AUTH_TOKEN + "?grant_type=%s" +
            "&refresh_token=%s&client_id=%s&client_secret=%s";

    private static final String URL_GENERATE_TOKEN = URL_AUTH_TOKEN + "?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s";

    private static final String BASE_API_URL = "https://gitlab.com/api/v4";

    private static final String URL_GET_GROUPS = BASE_API_URL + "/groups?top_level_only=true";

    private static final String URL_GET_PROJECTS = BASE_API_URL + "/groups/%s/projects" +
            "?include_subgroups=true";

    private static final String BASE_DB_KEY = "gitlab.com";

    private static final String SCOPES ="api";

    private static final String GRANT_TYPE ="refresh_token";

    private static final String URL_GET_WEBHOOKS = BASE_API_URL + "/projects/%s/hooks";

    private static final String URL_DELETE_WEBHOOK = BASE_API_URL + "/projects/%s/hooks/%s";

    private static final String URL_WEBHOOK = BASE_API_URL + "/projects/%s/hooks?url=%s&token=%s&merge_requests_events=true&push_events=true";

    private static final String URL_VALIDATE_TOKEN = "https://gitlab.com/api/v4/user";

    private static final String TOKEN_REQUEST_USER_AGENT = "CxIntegrations";

    private static final String MISSING_DATA = "CxFlow configuration settings validation failure: missing data.";

    public GitLabService(RestWrapper restWrapper, DataService dataStoreService, AccessTokenService tokenService) {
        super(restWrapper, dataStoreService, tokenService);
    }


    @Override
    public String getScopes() {
        return SCOPES;
    }

    public String getBaseDbKey() {
        return BASE_DB_KEY;
    }

    @Override
    public List<OrganizationWebDto> getOrganizations(@NonNull String authCode) {
        AccessTokenGitlabDto tokenFromGitlabApi = generateAccessToken(authCode);
        TokenInfoDto tokenForSaving = toStandardTokenDto(tokenFromGitlabApi);
        long tokenId = tokenService.createTokenInfo(tokenForSaving);
        return getAndStoreOrganizations(tokenForSaving.getAccessToken(), tokenId);
    }

    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {
        TokenInfoDto tokenInfo = tokenService.getTokenInfo(getBaseDbKey(), orgId);
        String accessToken = tokenInfo.getAccessToken();

        RepoGitlabDto[] reposFromGitlab = getReposFromGitLab(orgId, accessToken);

        OrgReposDto reposForDataStore = getReposForDataStore(accessToken, reposFromGitlab, orgId);

        dataStoreService.updateScmOrgRepo(reposForDataStore);

        return getReposForWebClient(reposForDataStore);
    }

    private List<RepoWebDto> getReposForWebClient(OrgReposDto reposForDataStore) {
        return reposForDataStore.getRepoList()
                .stream()
                .map(toRepoForWebClient())
                .collect(Collectors.toList());
    }

    private OrgReposDto getReposForDataStore(String accessToken, RepoGitlabDto[] reposFromGitlab, String orgId) {
        List<RepoDto> repos = Arrays.stream(reposFromGitlab)
                .map(toRepoForDataStore(accessToken))
                .collect(Collectors.toList());

        return OrgReposDto.builder()
                .orgIdentity(orgId)
                .scmUrl(getBaseDbKey())
                .repoList(repos)
                .build();
    }

    private Function<RepoDto, RepoWebDto> toRepoForWebClient() {
        return repo -> RepoWebDto.builder()
                .id(repo.getRepoIdentity())
                .name(repo.getName())
                .webhookEnabled(repo.isWebhookConfigured())
                .webhookId(repo.getWebhookId())
                .build();
    }

    private Function<RepoGitlabDto, RepoDto> toRepoForDataStore(String accessToken) {
        return (RepoGitlabDto repo) -> {
            RepoDto repoForDataStore = new RepoDto();
            repoForDataStore.setRepoIdentity(repo.getId());
            repoForDataStore.setName(normalizeName(repo.getName()));
            setWebhookRelatedFields(repo.getId(), accessToken, repoForDataStore);
            return repoForDataStore;
        };
    }

    private static String normalizeName(String gitlabProjectName) {
        final String SEPARATOR = "/";

        String result = StringUtils.defaultString(gitlabProjectName);
        if (result.contains(SEPARATOR)) {
            // Expecting GitLab project name in the form: "MyGroup / MySubgroup / MyProject".
            // By convention, we use all but the root group as CxIntegrations repo name, e.g. "MySubgroup / MyProject".
            result = StringUtils.substringAfter(result, SEPARATOR);
        }
        return result.trim();
    }

    private void setWebhookRelatedFields(String repoId, String accessToken, RepoDto target) {
        WebhookGitLabDto webhook = getRepositoryCxFlowWebhook(repoId, accessToken);
        if (webhook != null) {
            target.setWebhookConfigured(true);
            target.setWebhookId(webhook.getId());
        } else {
            target.setWebhookConfigured(false);
        }
    }

    private RepoGitlabDto[] getReposFromGitLab(String orgId, String accessToken) {
        String url = String.format(URL_GET_PROJECTS, orgId);

        ResponseEntity<RepoGitlabDto[]> response = restWrapper.sendBearerAuthRequest(
                url, HttpMethod.GET, null, null, RepoGitlabDto[].class, accessToken);

        return Objects.requireNonNull(response.getBody());
    }

    @Override
    public BaseDto createWebhook(@NonNull String orgId, @NonNull String repoId) {
        TokenInfoDto tokenInfo = tokenService.getTokenInfo(getBaseDbKey(), orgId);

        WebhookGitLabDto newWebhook = createWebhookInGitLab(repoId, tokenInfo.getAccessToken());
        validateWebhookDto(newWebhook);

        updateRepoInDataStore(orgId, repoId, newWebhook);

        return new BaseDto(newWebhook.getId());
    }

    private void updateRepoInDataStore(String orgId, String repoId, WebhookGitLabDto newWebhook) {
        RepoDto updateRepoRequest = RepoDto.builder()
                .isWebhookConfigured(true)
                .repoIdentity(repoId)
                .webhookId(newWebhook.getId())
                .build();

        dataStoreService.updateRepo2(getBaseDbKey(), orgId, updateRepoRequest);
    }

    private WebhookGitLabDto createWebhookInGitLab(String repoId, String accessToken) {
        String path = String.format(URL_WEBHOOK, repoId, getCxFlowUrl(), "1234");
        ResponseEntity<WebhookGitLabDto> response = restWrapper.sendBearerAuthRequest(path,
                HttpMethod.POST, new WebhookGitLabDto(), null, WebhookGitLabDto.class, accessToken);

        return Objects.requireNonNull(response.getBody(), "Missing webhook creation response.");
    }

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String deleteUrl) {
        String path = String.format(URL_DELETE_WEBHOOK, repoId, deleteUrl);
        super.deleteWebhook(orgId,repoId,path,WebhookGitLabDto.class);
    }

    @Override
    public CxFlowConfigDto getCxFlowConfiguration(@NonNull String orgId) {
        TokenInfoDto tokenInfo = tokenService.getTokenInfo(getBaseDbKey(), orgId);
        CxFlowConfigDto result = getOrganizationSettings(orgId, tokenInfo.getAccessToken());

        validateFieldsArePresent(result);
        ensureTokenIsValid(tokenInfo, result);

        return result;
    }

    private void ensureTokenIsValid(TokenInfoDto tokenInfo, CxFlowConfigDto targetConfig) {
        if (!isTokenValid(tokenInfo.getAccessToken())) {
            TokenInfoDto newTokenInfo = getRefreshedToken(tokenInfo);
            tokenService.updateTokenInfo(newTokenInfo);
            targetConfig.setScmAccessToken(newTokenInfo.getAccessToken());
        }
    }

    private TokenInfoDto getRefreshedToken(TokenInfoDto tokenInfo) {
        String refreshApiPath = buildRefreshTokenApiPath(tokenInfo.getRefreshToken());
        AccessTokenGitlabDto apiResponse = sendAccessTokenRequest(refreshApiPath, getHeadersAccessToken());
        TokenInfoDto result = toStandardTokenDto(apiResponse);
        result.setId(tokenInfo.getId());
        return result;
    }

    private static TokenInfoDto toStandardTokenDto(AccessTokenGitlabDto gitLabSpecificDto) {
        TokenInfoDto result = TokenInfoDto.builder()
                .accessToken(gitLabSpecificDto.getAccessToken())
                .refreshToken(gitLabSpecificDto.getRefreshToken())
                .build();

        // Currently not used in code, but may be useful for diagnostics.
        result.getAdditionalData().put("created_at", gitLabSpecificDto.getCreatedAt().toString());
        return result;
    }

    private List<OrganizationWebDto> getAndStoreOrganizations(String accessToken, long tokenId) {
        List<GroupGitlabDto> groups = getUserGroups(accessToken);
        List<OrgDto2> dataStoreOrgs = toDataStoreOrganizations(groups, tokenId);
        dataStoreService.storeOrgs2(getBaseDbKey(), dataStoreOrgs);

        return toOrganizationsForWebClient(groups);
    }

    private List<GroupGitlabDto> getUserGroups(String accessToken) {
        ResponseEntity<GroupGitlabDto[]> response =
                restWrapper.sendBearerAuthRequest(URL_GET_GROUPS, HttpMethod.GET, null, null,
                                                  GroupGitlabDto[].class, accessToken);

        return Arrays.asList(Objects.requireNonNull(response.getBody()));
    }

    private List<OrganizationWebDto> toOrganizationsForWebClient(List<GroupGitlabDto> gitlabOrgs) {
        return gitlabOrgs.stream()
                .map(gitlabOrg -> OrganizationWebDto.builder()
                        .id(gitlabOrg.getPath())
                        .name(gitlabOrg.getName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<OrgDto2> toDataStoreOrganizations(List<GroupGitlabDto> gitlabOrgs, long tokenId) {
        return gitlabOrgs.stream()
                .map(gitlabOrg -> OrgDto2.builder()
                        .orgIdentity(gitlabOrg.getPath())
                        .tokenId(tokenId)
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isTokenValid(String token) {
        boolean result = false;
        try {
            restWrapper.sendBearerAuthRequest(URL_VALIDATE_TOKEN, HttpMethod.GET, null, null,
                    CxFlowConfigDto.class,
                    token);
            result = true;
            log.info("Gitlab token validation passed successfully!");
        } catch (HttpClientErrorException ex) {
            log.info("Current token is not valid.");
        }
        return result;
    }

    private void validateFieldsArePresent(CxFlowConfigDto cxFlowConfigDto) {
        if (StringUtils.isAnyEmpty(cxFlowConfigDto.getScmAccessToken(),
                cxFlowConfigDto.getTeam(),
                cxFlowConfigDto.getCxgoSecret())) {
            log.error(MISSING_DATA);
            throw new ScmException(MISSING_DATA);
        }
    }

    private String buildRefreshTokenApiPath(String refreshToken) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        return String.format(URL_REFRESH_TOKEN, GRANT_TYPE, refreshToken,
                             scmDto.getClientId(), scmDto.getClientSecret());
    }

    private WebhookGitLabDto getRepositoryCxFlowWebhook(@NonNull String repoId,
                                                        @NonNull String accessToken){
        String path = String.format(URL_GET_WEBHOOKS, repoId);
        ResponseEntity<WebhookGitLabDto[]> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                WebhookGitLabDto[].class, accessToken);
        ArrayList<WebhookGitLabDto> webhookDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));

        return (WebhookGitLabDto) getActiveHook(webhookDtos);
    }


    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitLab api
     *
     * @param authCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitLab
     */
    private AccessTokenGitlabDto generateAccessToken(String authCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());

        String path = buildPathAccessToken(authCode, scmDto);
        AccessTokenGitlabDto tokenResponse = sendAccessTokenRequest(path, getHeadersAccessToken());
        log.info("Access token generated successfully");

        return tokenResponse;
    }

    private AccessTokenGitlabDto sendAccessTokenRequest(String path, Map<String, String> headers) {
        ResponseEntity<AccessTokenGitlabDto> response = restWrapper.sendRequest(path,
                HttpMethod.POST,
                null,
                headers,
                AccessTokenGitlabDto.class);

        AccessTokenGitlabDto tokenGitlabDto = Objects.requireNonNull(
                response.getBody(), "Missing access token generation response.");

        if (!verifyAccessToken(tokenGitlabDto)) {
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return tokenGitlabDto;
    }

    private Map<String, String> getHeadersAccessToken() {
        // If we don't specify any User-Agent, the request will fail with "403 Forbidden: [error code: 1010]".
        // This issue may not exist in some execution environments.
        return Collections.singletonMap(HttpHeaders.USER_AGENT, GitLabService.TOKEN_REQUEST_USER_AGENT);
    }

    private String buildPathAccessToken(String oAuthCode, ScmDto scmDto) {
        return String.format(URL_GENERATE_TOKEN, scmDto.getClientId(),
                             scmDto.getClientSecret(),
                             oAuthCode,
                             getRedirectUrl());
    }


}
