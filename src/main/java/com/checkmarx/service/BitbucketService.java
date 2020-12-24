package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.BaseDto;

import com.checkmarx.dto.bitbucket.*;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.gitlab.AccessTokenGitlabDto;

import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.AccessTokenManager;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;


import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.URLEncoder;

@Slf4j
@Service("bitbucket")
public class BitbucketService extends AbstractScmService implements ScmService  {

    private static final String API_VERSION = "/2.0";
    
    private static final String URL_AUTH_TOKEN = "https://bitbucket.org/site/oauth2/access_token";

    private static final String URL_REFRESH_TOKEN = URL_AUTH_TOKEN + "?grant_type=%s" +
            "&refresh_token=%s&client_id=%s&client_secret=%s";

    private static final String BASE_API_URL = "https://api.bitbucket.org";

    private static final String URL_GET_WORKSPACES = BASE_API_URL + API_VERSION + "/workspaces";

    private static final String URL_GET_REPOSITORIES =  BASE_API_URL + API_VERSION  + "/repositories/%s" +
            "?include_subgroups=true";

    private static final String BASE_DB_KEY = "bitbucket.com";

    private static final String SCOPES ="";
    

    private static final String URL_GET_WEBHOOKS = BASE_API_URL + API_VERSION  + "/repositories/%s/%s/hooks";

     private static final String URL_CREATE_WEBHOOK = BASE_API_URL + API_VERSION  +  "/repositories/%s/%s/hooks";

    private static final String URL_DELETE_WEBHOOK = BASE_API_URL + API_VERSION  +  "/repositories/%s/%s/hooks/%s";

    private static final String URL_VALIDATE_TOKEN = "https://gitlab.com/api/v4/user";

    
    
    @Override
    public String getScopes() {
        return SCOPES;
    }

    public String getBaseDbKey() {
        return BASE_DB_KEY;
    }

    @Override
    public List<OrganizationWebDto> getOrganizations(@NonNull String authCode) {
        AccessTokenBitbucketDto accessToken = generateAccessToken(authCode);
        log.info("Access token generated successfully");
        return getAndStoreOrganizations(accessToken);
    }


    @Override
    public List<RepoWebDto> getScmOrgRepos(@NonNull String workspaceId) {
        AccessTokenManager accessTokenManager = new AccessTokenManager(getBaseDbKey(), workspaceId, dataStoreService);
        String path = String.format(URL_GET_REPOSITORIES, workspaceId);
        ResponseEntity<RepoBitbucketListDto> response =  restWrapper
                .sendBearerAuthRequest(path, HttpMethod.GET,
                                       null, null,
                        RepoBitbucketListDto.class, accessTokenManager.getAccessTokenStr());
        List<RepoBitbucketDto> repoDtos = 
                Objects.requireNonNull(response.getBody()).getElements();
        for (RepoBitbucketDto repoDto : repoDtos) {
            WebhookBitbucketDto webhookDto = getRepositoryCxFlowWebhook(repoDto.getId(),workspaceId,
                    accessTokenManager.getAccessTokenStr());
            setWebhookDetails(repoDto, webhookDto);
        }
        OrgReposDto orgReposDto = Converter.convertToOrgRepoDto(accessTokenManager.getDbDto(), repoDtos);
        dataStoreService.updateScmOrgRepo(orgReposDto);
        return Converter.convertToListRepoWebDto(repoDtos);
    }
    

    @Override
    public BaseDto createWebhook(@NonNull String orgId, @NonNull String repoId ) {
        AccessTokenManager accessTokenManager = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);
        String path = String.format(URL_CREATE_WEBHOOK, orgId, repoId) ;
         ResponseEntity<WebhookBitbucketDto> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.POST,
                 getHookDto(repoId), null, WebhookBitbucketDto.class, accessTokenManager.getAccessTokenStr());
        WebhookBitbucketDto webhookDto = response.getBody();
        validateWebhookDto(webhookDto);
        dataStoreService.updateWebhook(repoId, accessTokenManager.getDbDto(), webhookDto.getId(), true);
        return new BaseDto(webhookDto.getId());
    }

    private WebhookBitbucketDto getHookDto(String repoId) {

        WebhookBitbucketDto hookdto = new WebhookBitbucketDto();

        hookdto.setDescription("CxFlow webhook");
        hookdto.setActive(true);
        hookdto.setUrl(getCxFlowUrl());
        hookdto.setEvents(BitBucketEvent.getAllEventsList());
        hookdto.setType("webhook_subscription");

        return hookdto;

    }
    
    @Override
    public void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String webhookId) {
        String path = String.format(URL_DELETE_WEBHOOK, orgId, repoId, webhookId);
        deleteWebhook(orgId, repoId, path, null);
    }

    @Override
    public CxFlowConfigDto getCxFlowConfiguration(@NonNull String orgId) {
        //CxFlow send org name, Using DataStore to get org id
        AccessTokenManager accessTokenManager = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);
        CxFlowConfigDto cxFlowConfigDto = getOrganizationSettings(orgId,accessTokenManager.getAccessTokenJson());
        Object accessTokenGitlabDto  = accessTokenManager.getFullAccessToken(AccessTokenGitlabDto.class);
        return validateCxFlowConfig(cxFlowConfigDto, (AccessTokenGitlabDto)accessTokenGitlabDto);

    }
    
    private List<OrganizationWebDto> getAndStoreOrganizations(AccessTokenBitbucketDto token) {
        ResponseEntity<BitbucketBaseListDto> response =
                restWrapper.sendBearerAuthRequest(URL_GET_WORKSPACES, HttpMethod.GET, null, null,
                        BitbucketBaseListDto.class, token.getAccessToken());
        List<BitbucketBase> organizationWebDtos = response.getBody().getElements();
        String tokenJson = Converter.convertObjectToJson(token);
        List<OrgDto> orgDtos =
                Converter.convertToListOrg(tokenJson, organizationWebDtos, getBaseDbKey());
        dataStoreService.storeOrgs(orgDtos);

        return Converter.convertToListOrgWebDtos(organizationWebDtos);
    }

    private CxFlowConfigDto validateCxFlowConfig(CxFlowConfigDto cxFlowConfigDto, AccessTokenGitlabDto token) {
        if(StringUtils.isEmpty(cxFlowConfigDto.getScmAccessToken()) || StringUtils.isEmpty(cxFlowConfigDto.getTeam()) || StringUtils.isEmpty(cxFlowConfigDto.getCxgoSecret())) {
            log.error("CxFlow configuration settings validation failure, missing data");
            throw new ScmException("CxFlow configuration settings validation failure, missing data");
        }
        try {
            restWrapper.sendBearerAuthRequest(URL_VALIDATE_TOKEN, HttpMethod.GET, null, null,
                                              CxFlowConfigDto.class,
                                              cxFlowConfigDto.getScmAccessToken());
            log.info("Gitlab token validation passed successfully!");
        } catch (HttpClientErrorException ex){
            token = refreshToken(token);
            cxFlowConfigDto.setScmAccessToken(token.getAccessToken());
            log.info("Gitlab refresh token process passed successfully!");
        }
        return cxFlowConfigDto;
    }

    private AccessTokenGitlabDto refreshToken(AccessTokenGitlabDto token) {
        //TODO 
//        token = sendRefreshTokenRequest(token.getRefreshToken());
//        getAndStoreOrganizations(token);
        return token;
    }



//    private AccessTokenGitlabDto sendRefreshTokenRequest(String refreshToken) {
//        String path = buildRefreshTokenPath(refreshToken);
//        return sendAccessTokenRequest(path, getHeadersAccessToken());
//    }

//    private String buildRefreshTokenPath(String refreshToken) {
//        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
//        return String.format(URL_REFRESH_TOKEN, GRANT_TYPE, refreshToken,
//                             scmDto.getClientId(), scmDto.getClientSecret());
//    }

    private WebhookBitbucketDto getRepositoryCxFlowWebhook(@NonNull String repoId, @NonNull String workspaceId,
                                                           @NonNull String accessToken){
        String path = String.format(URL_GET_WEBHOOKS, workspaceId, repoId);
        ResponseEntity<WebhookBitbucketListDto> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                WebhookBitbucketListDto.class, accessToken);
        WebhookBitbucketListDto webhookDtos = Objects.requireNonNull(response.getBody());

        return (WebhookBitbucketDto)getActiveHook(webhookDtos.getElements());
    }




    /**
     * generateAccessToken method using OAuth code, client id and client secret generates access
     * token via GitHub api
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return Access token given from GitHub
     */
    private AccessTokenBitbucketDto generateAccessToken(String oAuthCode) {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(scmDto.getClientId(), scmDto.getClientSecret(), StandardCharsets.UTF_8);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return generateAccessToken(restWrapper, URL_AUTH_TOKEN, headers,
                getBodyAccessToken(oAuthCode));
    }


    private AccessTokenBitbucketDto generateAccessToken(RestWrapper restWrapper, String path, HttpHeaders headers, MultiValueMap<String, String> body) {
        ResponseEntity<AccessTokenBitbucketDto> response = sendAccessTokenRequest(restWrapper, path, headers, body);

        AccessTokenBitbucketDto accessTokenDto = response.getBody();
        if(!verifyAccessToken(accessTokenDto)){
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return accessTokenDto;
    }

    private ResponseEntity<AccessTokenBitbucketDto> sendAccessTokenRequest(RestWrapper restWrapper, String path, HttpHeaders headers, MultiValueMap<String, String> body) {
        return  restWrapper.sendUrlEncodedPostRequest(path,body, headers, AccessTokenBitbucketDto.class);
    }


    private MultiValueMap<String, String> getBodyAccessToken(String oAuthCode) {

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();

        map.put("grant_type",  Collections.singletonList("authorization_code"));
        map.put("code", Collections.singletonList(oAuthCode));

        return map;
    }



    
    
}
