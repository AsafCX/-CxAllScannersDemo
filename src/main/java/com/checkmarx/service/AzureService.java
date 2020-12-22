package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.BaseDto;
import com.checkmarx.dto.azure.*;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.OrgDto;
import com.checkmarx.dto.datastore.OrgReposDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.gitlab.WebhookGitLabDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.AccessTokenManager;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;


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
    

    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitHub api
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitHub
     */
    private AccessTokenAzureDto generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        return generateAccessToken(restWrapper, AzureService.URL_AUTH_TOKEN, null,
                                               getBodyAccessToken(oAuthCode, scmDto));
    }


    private AccessTokenAzureDto generateAccessToken(RestWrapper restWrapper, String path, Map<String, String> headers, MultiValueMap<String, String> body) {
        ResponseEntity<AccessTokenAzureDto> response = sendAccessTokenRequest(restWrapper, path, headers, body);

        AccessTokenAzureDto accessTokenDto = response.getBody();
        if(!verifyAccessToken(accessTokenDto)){
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return accessTokenDto;
    }

    private ResponseEntity<AccessTokenAzureDto> sendAccessTokenRequest(RestWrapper restWrapper, String path, Map<String, String> headers, MultiValueMap<String, String> body) {
        return  restWrapper.sendUrlEncodedPostRequest(path, 
                body, headers,AccessTokenAzureDto.class);
    }


    private MultiValueMap<String, String> getBodyAccessToken(String oAuthCode, ScmDto scmDto) {

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();

        map.put("client_assertion_type", Collections.singletonList("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
        map.put("client_assertion",  Collections.singletonList(scmDto.getClientSecret()));
        map.put("grant_type",  Collections.singletonList("urn:ietf:params:oauth:grant-type:jwt-bearer"));
        map.put("assertion", Collections.singletonList(oAuthCode));
        map.put("redirect_uri", Collections.singletonList(getRedirectUrl()));
        return map;
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
        AccessTokenAzureDto accessToken = generateAccessToken(authCode);
        log.info("Access token generated successfully");

        ResponseEntity<BaseDto> responseId =
                restWrapper.sendBearerAuthRequest(URL_GET_USER_ID, HttpMethod.GET, null, null,
                        BaseDto.class, accessToken.getAccessToken());
        BaseDto userId = Objects.requireNonNull(responseId.getBody());

        String urlAccounts = String.format(URL_GET_USER_ACCOUNTS, userId.getId());
        
        ResponseEntity<AzureUserOrganizationsDto> response =
                restWrapper.sendBearerAuthRequest(urlAccounts, HttpMethod.GET, null, null,
                        AzureUserOrganizationsDto.class, accessToken.getAccessToken());

        AzureUserOrganizationsDto azureUserOrganizationsDto = Objects.requireNonNull(response.getBody());
        String tokenJson = Converter.convertObjectToJson(accessToken);
         List<OrgDto> orgDtos = Converter.convertToListOrg(tokenJson,
                                                      azureUserOrganizationsDto.getOrganizations(), getBaseDbKey());
        dataStoreService.storeOrgs(orgDtos);
        return Converter.convertToListOrgWebDtos(azureUserOrganizationsDto.getOrganizations());
    }
    
    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {
        AccessTokenManager accessTokenWrapper = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);
        String urlProjectsApi = String.format(URL_GET_ALL_PROJECTS, orgId);
        ResponseEntity<AzureProjectsDto> responseProjects =  restWrapper
                .sendBearerAuthRequest(urlProjectsApi, HttpMethod.GET,
                        null, null,
                        AzureProjectsDto.class, accessTokenWrapper.getAccessTokenStr());

        AzureProjectsDto azureProjectsIds = Objects.requireNonNull(responseProjects.getBody());

        ArrayList<RepoAzureDto>  projectsAndReposHooks = new ArrayList<>();

        List<AzureWebhookDto> orgHooks = getOrganizationCxFlowHooks(orgId, accessTokenWrapper.getAccessTokenStr());

        for (int i=0; i<azureProjectsIds.getCount() && azureProjectsIds.getProjectIds()!=null ; i++) {

            RepoAzureDto project = azureProjectsIds.getProjectIds().get(i);
            Map<String, List<String>> repoHooks = getHooksOnRepoLevel(orgHooks, project.getId());
            RepoListAzureDto projectRepos = getProjectRepos(orgId, accessTokenWrapper.getAccessTokenStr(), project.getId());

            if(projectRepos.getCount()>0 && projectRepos.getRepos()!=null) {
                setAdditionalDetails(repoHooks, projectRepos, project);
                projectsAndReposHooks.addAll(projectRepos.getRepos());
            }
            
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(accessTokenWrapper.getDbDto(), projectsAndReposHooks);
        dataStoreService.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(projectsAndReposHooks);
    }


    

    private Map<String, List<String>> getHooksOnRepoLevel(List<AzureWebhookDto> organizationHooks,
                                                          String projectId) {

        Map<String, List<String>> repoHooks = new HashMap<>();
        
        organizationHooks.stream().forEach(projectHook -> {
            if(projectHook.getProjectId().equals(projectId) && !StringUtils.isEmpty(projectHook.getRepositoryId())){
                //hook on repo level
                //project level hooks - will be skipped
                if(repoHooks.get(projectHook.getRepositoryId()) == null){
                    repoHooks.put(projectHook.getRepositoryId(), new LinkedList<>());
                }
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

    private void setAdditionalDetails(Map<String, List<String>> cxFlowHooks, RepoListAzureDto repoAzureDtos, RepoAzureDto project) {
        for (RepoAzureDto repository : repoAzureDtos.getRepos()) {

            if (cxFlowHooks.containsKey(repository.getId())) {
                List<String> listHooks = cxFlowHooks.get(repository.getId());
                BaseDto multipleHookId = new BaseDto();
                listHooks.stream().forEach(id -> multipleHookId.join(id));
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
        String path = String.format(URL_CREATE_WEBHOOK, orgId, getCxFlowWebHook(), accessTokenWrapper.getAccessTokenStr()) ;

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
        //TODO
        return null;
    }

    private List<AzureWebhookDto> getOrganizationCxFlowHooks(@NonNull String orgId,
                                                                                 @NonNull String accessToken){
        String path = String.format(URL_GET_WEBHOOKS, orgId);  
        
        ResponseEntity<WebhookListAzureDto> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                WebhookListAzureDto.class, accessToken);
        
        WebhookListAzureDto allHooks =(response.getBody());

        List<AzureWebhookDto> cxFlowHooks = new LinkedList<>();
        for (int i=0; i< allHooks.getCount() && allHooks.getWebhooks()!= null; i++) {

            AzureWebhookDto webhookDto = allHooks.getWebhooks().get(i);
            if (webhookDto != null  &&  webhookDto.getConsumerInputs()!=null && webhookDto.getConsumerInputs().getUrl().contains(getCxFlowWebHook()) && webhookDto.isPushOrPull())
                cxFlowHooks.add(webhookDto);
        }
        return cxFlowHooks;
    }

    private AzureWebhookDto generateHookData(String repoId, String projectId, AzureEvent event)  {
       
        String targetAppUrl =  String.format(event.getHookUrl(), getCxFlowWebHook());
                
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
