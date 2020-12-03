package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.BaseDto;
import com.checkmarx.dto.IRepoDto;
import com.checkmarx.dto.azure.AzureUserOrganizationsDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.OrgReposDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.gitlab.RepoGitlabDto;
import com.checkmarx.dto.gitlab.WebhookGitLabDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service("azure")
public class AzureService extends AbstractScmService implements ScmService  {

    private static final String API_VERSION = "6.0";
    
    private static final String BASE_API_URL = "https://app.vssps.visualstudio.com";
    
    private static final String URL_AUTH_TOKEN = BASE_API_URL + "/oauth2/token";

    private static final String URL_GET_USER_ID = BASE_API_URL + "/_apis/profile/profiles/me?api-version=" + API_VERSION;
            
    private static final String URL_GET_USER_ACCOUNTS =  BASE_API_URL + "/_apis/accounts?api-version=" + API_VERSION + "&memberId=%s";

    private static final String URL_GET_REPOS =  "/projects?simple=true&membership=true";

    private static final String BASE_DB_KEY = "azure.com";

    private static final String SCOPES ="vso.code_full vso.project_manage";

    private static final String URL_GET_WEBHOOKS =  "/projects/%s/hooks";

    private static final String URL_DELETE_WEBHOOK =  "/projects/%s/hooks/%s";

    private static final String URL_CREATE_WEBHOOK =  "/projects/%s/hooks?url=%s&token=%s&merge_requests_events=true&push_events=true";
    

    @Value("${redirect.url}")
    private String redirectUrl;
    

    @Override
    protected ResponseEntity<AccessTokenDto> sendAccessTokenRequest(RestWrapper restWrapper, String path, Map<String, String> headers, Object body) {
        return (ResponseEntity<AccessTokenDto>) restWrapper.sendUrlEncodedPostRequest(path, HttpMethod.POST,
                (MultiValueMap<String, String>)body, headers,
                AccessTokenDto.class);
    }

    @Override
    protected Object getBodyAccessToken(String oAuthCode, ScmDto scmDto) {

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();

        map.put("client_assertion_type", Arrays.asList("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
        map.put("client_assertion",  Arrays.asList(scmDto.getClientSecret()));
        map.put("grant_type",  Arrays.asList("urn:ietf:params:oauth:grant-type:jwt-bearer"));
        map.put("assertion",  Arrays.asList(oAuthCode));
        map.put("redirect_uri",  Arrays.asList(redirectUrl));
        return map;
    }

    @Override
    protected String buildPathAccessToken(String oAuthCode, ScmDto scmDto)
    {
        return AzureService.URL_AUTH_TOKEN;
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
        AccessTokenDto accessToken = generateAccessToken(authCode);
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
        
         List<ScmAccessTokenDto> scmAccessTokenDtos =
                Converter.convertToListOrgAccessToken(accessToken, azureUserOrganizationsDto.getOrganizations(), getBaseDbKey());
        dataStoreService.storeScmOrgsToken(scmAccessTokenDtos);
        return Converter.convertToListOrgWebDtos(azureUserOrganizationsDto.getOrganizations());
    }


    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        String path = String.format(URL_GET_REPOS, orgId);
        ResponseEntity<RepoGitlabDto[]> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
                                       null, null,
                                       RepoGitlabDto[].class, scmAccessTokenDto.getAccessToken());
        ArrayList<RepoGitlabDto> repoGitlabDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        List<? extends IRepoDto> filteredRepos = repoGitlabDtos.stream().filter(repoGitlabDto -> repoGitlabDto.getNamespace().getId().equals(orgId)).collect(Collectors.toList());
        for (IRepoDto repoGitlabDto : filteredRepos) {
            WebhookGitLabDto webhookGitlabDto = getRepositoryCxFlowWebhook(repoGitlabDto.getId(),
                    scmAccessTokenDto.getAccessToken());
            if(webhookGitlabDto != null) {
                repoGitlabDto.setWebHookEnabled(true);
                repoGitlabDto.setWebhookId(webhookGitlabDto.getId());
            } else {
                repoGitlabDto.setWebHookEnabled(false);
            }
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(scmAccessTokenDto, filteredRepos);
        dataStoreService.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(filteredRepos);
    }


    @Override
    public String createWebhook(@NonNull String orgId, @NonNull String projectId ) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        String path = String.format(URL_CREATE_WEBHOOK, projectId, cxFlowWebHook, scmAccessTokenDto.getAccessToken()) ;
         ResponseEntity<WebhookGitLabDto> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.POST,
                 new WebhookGitLabDto(), null,
                 WebhookGitLabDto.class,
                scmAccessTokenDto.getAccessToken());
        WebhookGitLabDto webhookGitLabDto = response.getBody();
        validateResponse(webhookGitLabDto);
        dataStoreService.updateWebhook(projectId, scmAccessTokenDto, webhookGitLabDto.getId(), true);
        return webhookGitLabDto.getId();
    }

    private void validateResponse(WebhookGitLabDto webhookGitLabDto) {
        if(webhookGitLabDto == null || StringUtils.isEmpty(webhookGitLabDto.getId())){
            log.error(RestWrapper.WEBHOOK_CREATE_FAILURE);
            throw new ScmException(RestWrapper.WEBHOOK_CREATE_FAILURE);
        }
    }

    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String webhookId) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(getBaseDbKey(), orgId);
        String path = String.format(URL_DELETE_WEBHOOK, repoId, webhookId);

        try {
            restWrapper.sendBearerAuthRequest(path, HttpMethod.DELETE,null, null,
                    WebhookGitLabDto.class,
                    scmAccessTokenDto.getAccessToken());
        } catch (HttpClientErrorException ex){
            if(ex.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                log.error("Webhook not found: {}", ex.getMessage());
                throw new ScmException(RestWrapper.WEBHOOK_DELETE_FAILURE);
            }
            throw new ScmException(RestWrapper.GENERAL_RUNTIME_EXCEPTION);
        }
        dataStoreService.updateWebhook(repoId, scmAccessTokenDto, null, false);
    }
    

    @Override
    public CxFlowConfigDto validateCxFlowConfiguration(@NonNull CxFlowConfigDto cxFlowConfigDto) {
        //TODO
        return null;
    }



    private WebhookGitLabDto getRepositoryCxFlowWebhook(@NonNull String repoId,
                                                        @NonNull String accessToken){
        String path = String.format(URL_GET_WEBHOOKS, repoId);
        ResponseEntity<WebhookGitLabDto[]> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                WebhookGitLabDto[].class, accessToken);
        ArrayList<WebhookGitLabDto> webhookGitLabDtos = new ArrayList<>(Arrays.asList(
                Objects.requireNonNull(response.getBody())));
        for (WebhookGitLabDto webhookGitLabDto : webhookGitLabDtos) {
            if (webhookGitLabDto != null  && webhookGitLabDto.getUrl().equals(cxFlowWebHook))
                return webhookGitLabDto;
        }
        return null;
    }


}
