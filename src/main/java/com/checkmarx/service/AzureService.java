package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.BaseDto;
import com.checkmarx.dto.azure.*;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.gitlab.WebhookGitLabDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.AccessTokenManager;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.UnknownContentTypeException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service("azure")
public class AzureService extends AbstractScmService implements ScmService  {

    private static final String API_VERSION = "6.0";
    
    private static final String BASE_HIGH_LEVEL_API_URL = "https://app.vssps.visualstudio.com";

    private static final String BASE_API_URL = "https://dev.azure.com";
    
    private static final String URL_AUTH_TOKEN = BASE_HIGH_LEVEL_API_URL + "/oauth2/token";

    private static final String URL_GET_USER_ID = BASE_HIGH_LEVEL_API_URL + "/_apis/profile/profiles/me?api-version=" + API_VERSION;
            
    private static final String URL_GET_USER_ACCOUNTS =  BASE_HIGH_LEVEL_API_URL + "/_apis/accounts?api-version=" + API_VERSION + "&memberId=%s";

    private static final String URL_GET_ALL_PROJECTS = BASE_API_URL +  "/%s/_apis/projects?api-version=" + API_VERSION;
    
    private static final String URL_GET_REPOS =  BASE_API_URL +  "/%s/%s/_apis/git/repositories?api-version=" + API_VERSION;

    private static final String BASE_DB_KEY = "azure.com";

    private static final String SCOPES ="vso.code_full vso.code_status vso.project_manage vso.threads_full vso.work_full";

    private static final String URL_GET_WEBHOOKS =    BASE_API_URL + "/%s/_apis/hooks/subscriptions?api-version=" + API_VERSION;

    private static final String URL_DELETE_WEBHOOK =  BASE_API_URL + "/%s/_apis/hooks/subscriptions/%s?api-version=" + API_VERSION;

    private static final String URL_CREATE_WEBHOOK =  BASE_API_URL + "/%s/_apis/hooks/subscriptions?api-version=" + API_VERSION;
    private static final String AZURE_CONSUMER_USERNAME = "cxflow";
    private static final String AZURE_CONSUMER_PASSWORD = "1234";

    public AzureService(RestWrapper restWrapper, DataService dataStoreService, AccessTokenService tokenService) {
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
        AccessTokenAzureDto tokenFromScmApi = generateAccessToken(authCode);
        TokenInfoDto tokenForSaving = toStandardTokenDto(tokenFromScmApi);
        long tokenId = tokenService.createTokenInfo(tokenForSaving);
        return getAndStoreOrganizations(tokenForSaving.getAccessToken(), tokenId);
    }

    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {
        TokenInfoDto tokenInfo = tokenService.getTokenInfo(getBaseDbKey(), orgId);
        String accessToken = tokenInfo.getAccessToken();

        List<RepoAzureDto> projects = getProjectsFromScm(orgId, accessToken);
        List<RepoAzureDto> repos = getReposWithWebhookInfo(orgId, accessToken, projects);
        OrgReposDto reposForDataStore = getReposForDataStore(repos, orgId);
        dataStoreService.updateScmOrgRepo(reposForDataStore);

        return getReposForWebClient(reposForDataStore);
    }

    private List<RepoAzureDto> getReposWithWebhookInfo(String orgId, String accessToken, List<RepoAzureDto> projects) {
        List<AzureWebhookDto> orgHooks = getOrganizationCxFlowHooks(orgId, accessToken);
        List<RepoAzureDto> projectsAndReposHooks = new ArrayList<>();
        for (RepoAzureDto project : projects) {
            Map<String, List<String>> repoHooks = getHooksOnRepoLevel(orgHooks, project.getId());
            RepoListAzureDto projectRepos = getProjectRepos(orgId, accessToken, project.getId());
            List<RepoAzureDto> repos = projectRepos.getRepos();
            if (projectRepos.getCount() > 0 && repos != null) {
                setAdditionalDetails(repoHooks, repos, project);
                projectsAndReposHooks.addAll(repos);
            }
        }
        return projectsAndReposHooks;
    }

    private OrgReposDto getReposForDataStore(List<RepoAzureDto> reposFromScm, String orgId) {
        List<RepoDto> repos = reposFromScm.stream()
                .map(toRepoForDataStore())
                .collect(Collectors.toList());

        return OrgReposDto.builder()
                .orgIdentity(orgId)
                .scmUrl(getBaseDbKey())
                .repoList(repos)
                .build();
    }

    private Function<RepoAzureDto, RepoDto> toRepoForDataStore() {
        return (RepoAzureDto repo) -> RepoDto.builder()
                .repoIdentity(repo.getId())
                .name(repo.getName())
                .isWebhookConfigured(repo.isWebHookEnabled())
                .webhookId(repo.getWebhookId())
                .build();
    }

    private List<RepoAzureDto> getProjectsFromScm(String orgId, String accessToken) {
        String urlProjectsApi = String.format(URL_GET_ALL_PROJECTS, orgId);
        ResponseEntity<AzureProjectsDto> response =  restWrapper
                .sendBearerAuthRequest(urlProjectsApi, HttpMethod.GET,
                        null, null,
                        AzureProjectsDto.class, accessToken);

        AzureProjectsDto projectsResponse = Objects.requireNonNull(response.getBody());
        return Objects.requireNonNull(projectsResponse.getValue());
    }

    private AccessTokenAzureDto generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());

        AccessTokenAzureDto tokenResponse = createOrRefreshAccessToken(AzureService.URL_AUTH_TOKEN, null,
                getBodyAccessToken(oAuthCode, scmDto));
        log.info("Access token generated successfully");

        return tokenResponse;
    }

    private AccessTokenAzureDto createOrRefreshAccessToken(String path, Map<String, String> headers, MultiValueMap<String, String> body) {
        ResponseEntity<AccessTokenAzureDto> response = restWrapper.sendUrlEncodedPostRequest(path, body, headers, AccessTokenAzureDto.class);

        AccessTokenAzureDto accessTokenDto = Objects.requireNonNull(response.getBody());
        if (!verifyAccessToken(accessTokenDto)) {
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return accessTokenDto;
    }

    private MultiValueMap<String, String> getBodyAccessToken(String oAuthCode, ScmDto scmDto) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();

        map.put("client_assertion_type", Collections
                .singletonList("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
        map.put("client_assertion", Collections.singletonList(scmDto.getClientSecret()));
        map.put("grant_type",
                Collections.singletonList("urn:ietf:params:oauth:grant-type:jwt-bearer"));
        map.put("assertion", Collections.singletonList(oAuthCode));
        map.put("redirect_uri", Collections.singletonList(getRedirectUrl()));
        return map;
    }

    private static TokenInfoDto toStandardTokenDto(AccessTokenAzureDto scmSpecificDto) {
        TokenInfoDto result = TokenInfoDto.builder()
                .accessToken(scmSpecificDto.getAccessToken())
                .refreshToken(scmSpecificDto.getRefreshToken())
                .build();

        if (scmSpecificDto.expiresIn != null) {
            result.getAdditionalData().put("expires_in", scmSpecificDto.expiresIn.toString());
        }
        return result;
    }

    private List<OrganizationWebDto> getAndStoreOrganizations(String accessToken, long tokenId) {
        String userId = getCurrentUserId(accessToken);
        List<AzureUserOrganizationsDto.Organization> userOrgs = getUserOrgs(userId, accessToken);
        List<OrgDto2> dataStoreOrgs = toDataStoreOrganizations(userOrgs, tokenId);
        dataStoreService.storeOrgs2(getBaseDbKey(), dataStoreOrgs);
        return toOrganizationsForWebClient(dataStoreOrgs);
    }

    private List<OrganizationWebDto> toOrganizationsForWebClient(List<OrgDto2> dataStoreOrgs) {
        return dataStoreOrgs.stream()
                .map(org -> OrganizationWebDto.builder()
                        .id(org.getOrgIdentity())
                        .name(org.getName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<OrgDto2> toDataStoreOrganizations(List<AzureUserOrganizationsDto.Organization> azureOrgs, long tokenId) {
        return azureOrgs.stream()
                .map(azureOrg -> OrgDto2.builder()
                        .orgIdentity(azureOrg.getId())
                        .tokenId(tokenId)
                        .name(azureOrg.getName())
                        .build())
                .collect(Collectors.toList());
    }

    private List<AzureUserOrganizationsDto.Organization> getUserOrgs(String userId, String accessToken) {
        String urlAccounts = String.format(URL_GET_USER_ACCOUNTS, userId);

        ResponseEntity<AzureUserOrganizationsDto> response =
                restWrapper.sendBearerAuthRequest(urlAccounts, HttpMethod.GET, null, null,
                                                  AzureUserOrganizationsDto.class, accessToken);

        AzureUserOrganizationsDto orgsWrapper = Objects.requireNonNull(response.getBody());
        return Objects.requireNonNull(orgsWrapper.getOrganizations());
    }

    private String getCurrentUserId(String accessToken) {
        ResponseEntity<BaseDto> responseId =
                restWrapper.sendBearerAuthRequest(URL_GET_USER_ID, HttpMethod.GET, null, null,
                                                  BaseDto.class, accessToken);
        BaseDto userProfile = Objects.requireNonNull(responseId.getBody());
        return Objects.requireNonNull(userProfile.getId());
    }


    

    private Map<String, List<String>> getHooksOnRepoLevel(List<AzureWebhookDto> organizationHooks,
                                                          String projectId) {

        Map<String, List<String>> repoHooks = new HashMap<>();
        
        organizationHooks.forEach(projectHook -> {
            if(projectHook.getProjectId().equals(projectId) && !StringUtils.isEmpty(projectHook.getRepositoryId())){
                //hook on repo level
                //project level hooks - will be skipped
                repoHooks.computeIfAbsent(projectHook.getRepositoryId(), k -> new LinkedList<>());
                repoHooks.get(projectHook.getRepositoryId()).add(projectHook.getHookId());
            }
        });
        
        return repoHooks;
       
    }

    private RepoListAzureDto getProjectRepos(@NonNull String orgId, String token, String projectId) {
        String urlReposApi = String.format(URL_GET_REPOS, orgId, projectId);

        ResponseEntity<RepoListAzureDto> response = restWrapper
                .sendBearerAuthRequest(urlReposApi, HttpMethod.GET,
                        null, null,
                        RepoListAzureDto.class, token);
        return Objects.requireNonNull(response.getBody());
    }

    private void setAdditionalDetails(Map<String, List<String>> cxFlowHooks, List<RepoAzureDto> repos, RepoAzureDto project) {
        for (RepoAzureDto repository : repos) {

            if (cxFlowHooks.containsKey(repository.getId())) {
                List<String> listHooks = cxFlowHooks.get(repository.getId());
                BaseDto multipleHookId = new BaseDto();
                listHooks.forEach(multipleHookId::join);
                repository.setWebHookEnabled(true);
                repository.setWebhookId(multipleHookId.getId());
            }

            repository.setId(new BaseDto(project.getId(),repository.getId()));
            
            if(!project.getName().trim().equals(repository.getName().trim())) {
                repository.setName(project.getName() + " / " + repository.getName());
            }
        }
    }


    @Override
    public BaseDto createWebhook(@NonNull String orgId, @NonNull String projectAndRepoIds ) {
        AccessTokenManager accessTokenWrapper = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);
        String path = String.format(URL_CREATE_WEBHOOK, orgId) ;

        List<String> listProjectAndRepo = getProjectAndRepoIds(projectAndRepoIds);
        
        String projectId = listProjectAndRepo.get(0);
        String repoId = listProjectAndRepo.get(1);
        
        BaseDto hookDtoHook1 = createHook(projectId, repoId, accessTokenWrapper.getAccessTokenStr(), path, AzureEvent.CREATE_PULL_REQEUST );
        BaseDto hookDtoHook2 = createHook(projectId, repoId, accessTokenWrapper.getAccessTokenStr(), path, AzureEvent.UPDATE_PULL_REQEUST);
        BaseDto hookDtoHook3 = createHook(projectId, repoId, accessTokenWrapper.getAccessTokenStr(), path, AzureEvent.PUSH);
        BaseDto hookDto = hookDtoHook1.join(hookDtoHook2).join(hookDtoHook3);
        dataStoreService.updateWebhook(repoId, accessTokenWrapper.getDbDto(), hookDto.getId(), true);
        return hookDto;
    }

    private List<String> getProjectAndRepoIds(@NonNull String projectAndRepoId) {
        List<String> listProjectAndRepo = new BaseDto(projectAndRepoId).split();

        if(listProjectAndRepo.size()!= 2){
            throw new ScmException("Invalid input to createWebhook. The input should consist of project and repository Ids");
        }
        return listProjectAndRepo;
    }

    private BaseDto createHook(@NonNull String projectId, @NonNull String repoId,
                               String token, String path, AzureEvent event)  {
        AzureWebhookDto hookData = generateHookData(repoId,projectId, event);
        ResponseEntity<BaseDto> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.POST,
                        hookData, null,
                        BaseDto.class, token);

        BaseDto hookDto = Objects.requireNonNull(response.getBody());
        validateResponse(hookDto);
        return hookDto;
    }

    private void validateResponse(BaseDto webhookGitLabDto) {
        if(webhookGitLabDto == null || StringUtils.isEmpty(webhookGitLabDto.getId())){
            log.error(RestWrapper.WEBHOOK_CREATE_FAILURE);
            throw new ScmException(RestWrapper.WEBHOOK_CREATE_FAILURE);
        }
    }

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String deleteUrl) {

       ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
     
        List<String> webhookIds = new BaseDto(deleteUrl).split();

        for (String currWebhookId:webhookIds) {
            String path = String.format(URL_DELETE_WEBHOOK, orgId, currWebhookId);
            super.deleteWebhook( orgId,  repoId, path, WebhookGitLabDto.class);

        }
        dataStoreService.updateWebhook(repoId, scmAccessTokenDto, null, false);
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
        AccessTokenAzureDto apiResponse = sendRefreshTokenRequest(tokenInfo.getRefreshToken());
        TokenInfoDto result = toStandardTokenDto(apiResponse);
        result.setId(tokenInfo.getId());
        return result;
    }

    private boolean isTokenValid(String token) {
        boolean result = false;
        try {
            restWrapper.sendBearerAuthRequest(URL_GET_USER_ID, HttpMethod.GET, null, null,
                    BaseDto.class, token);
            result = true;
            log.info("Access token validation passed successfully!");
        } catch (HttpClientErrorException | UnknownContentTypeException ex) {
            log.info("Current token is not valid.");
        }
        return result;
    }

    private AccessTokenAzureDto sendRefreshTokenRequest(String refreshToken) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        return createOrRefreshAccessToken(AzureService.URL_AUTH_TOKEN, null,
                                   getBodyRefreshAccessToken(refreshToken, scmDto));
    }

    private MultiValueMap<String, String> getBodyRefreshAccessToken(String refreshAccessToken,
                                                                    ScmDto scmDto) {

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();

        map.put("client_assertion_type", Collections.singletonList("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
        map.put("client_assertion",  Collections.singletonList(scmDto.getClientSecret()));
        map.put("grant_type",  Collections.singletonList("refresh_token"));
        map.put("assertion", Collections.singletonList(refreshAccessToken));
        map.put("redirect_uri", Collections.singletonList(getRedirectUrl()));
        return map;
    }

    private List<AzureWebhookDto> getOrganizationCxFlowHooks(@NonNull String orgId, @NonNull String accessToken) {
        String url = String.format(URL_GET_WEBHOOKS, orgId);

        ResponseEntity<WebhookListAzureDto> response = restWrapper.sendBearerAuthRequest(url, HttpMethod.GET,
                null, null,
                WebhookListAzureDto.class, accessToken);

        WebhookListAzureDto allHooks = Objects.requireNonNull(response.getBody());
        List<AzureWebhookDto> webhooks = Objects.requireNonNull(allHooks.getWebhooks());

        return webhooks.stream()
                .filter(this::belongsToCxFlow)
                .collect(Collectors.toList());
    }

    private boolean belongsToCxFlow(AzureWebhookDto webhook) {
        return webhook != null
                && webhook.getConsumerInputs() != null
                && webhook.getConsumerInputs().getUrl().contains(getCxFlowUrl())
                && webhook.isPushOrPull();
    }

    private AzureWebhookDto generateHookData(String repoId, String projectId, AzureEvent event)  {
       
        String targetAppUrl =  String.format(event.getHookUrl(), getCxFlowUrl());
                
        AzureWebhookDto.ConsumerInputs consumerInputs = AzureWebhookDto.ConsumerInputs.builder()
                .basicAuthUsername(AZURE_CONSUMER_USERNAME)
                .basicAuthPassword(AZURE_CONSUMER_PASSWORD)
                .url(targetAppUrl)
                .build();
        PublisherInputs publisherInput = PublisherInputs.builder()
                .projectId(projectId)
                .repository(repoId)
                .build();

        return AzureWebhookDto.builder()
                .consumerActionId("httpRequest")
                .consumerId("webHooks")
                .consumerInputs(consumerInputs)
                .eventType(event.getType())
                .publisherId("tfs")
                .publisherInputs(publisherInput)
                .resourceVersion("1.0")
                .scope(1)
                .build();
    }
    

}
