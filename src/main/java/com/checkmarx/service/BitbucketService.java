package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.BaseDto;
import com.checkmarx.dto.bitbucket.*;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.OrgDto;
import com.checkmarx.dto.datastore.OrgReposDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.AccessTokenManager;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service("bitbucket")
public class BitbucketService extends AbstractScmService implements ScmService  {

    private static final String API_VERSION = "/2.0";
    
    private static final String URL_AUTH_TOKEN = "https://bitbucket.org/site/oauth2/access_token";

    private static final String BASE_API_URL = "https://api.bitbucket.org";

    private static final String URL_GET_WORKSPACES = BASE_API_URL + API_VERSION + "/workspaces";

    private static final String URL_GET_REPOSITORIES =  BASE_API_URL + API_VERSION  + "/repositories/%s" +
            "?include_subgroups=true";

    private static final String BASE_DB_KEY = "bitbucket.com";

    private static final String SCOPES ="";
    

    private static final String URL_GET_WEBHOOKS = BASE_API_URL + API_VERSION  + "/repositories/%s/%s/hooks";

     private static final String URL_CREATE_WEBHOOK = BASE_API_URL + API_VERSION  + "/repositories/%s/%s/hooks";

    private static final String URL_DELETE_WEBHOOK = BASE_API_URL + API_VERSION  +  "/repositories/%s/%s/hooks/%s";

    private static final String URL_VALIDATE_TOKEN = BASE_API_URL + API_VERSION + "/user";

    public BitbucketService(RestWrapper restWrapper, DataService dataStoreService, AccessTokenService tokenService) {
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
                 getHookDto(), null, WebhookBitbucketDto.class, accessTokenManager.getAccessTokenStr());
        WebhookBitbucketDto webhookDto = response.getBody();
        validateWebhookDto(webhookDto);
        dataStoreService.updateWebhook(repoId, accessTokenManager.getDbDto(), webhookDto.getId(), true);
        return new BaseDto(webhookDto.getId());
    }

    private WebhookBitbucketDto getHookDto() {

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
        AccessTokenManager accessTokenManager = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);
        CxFlowConfigDto cxFlowConfigDto = getOrganizationSettings(orgId, accessTokenManager.getAccessTokenStr());
        Object accessTokenBitbucketDto  =
                accessTokenManager.getFullAccessToken(AccessTokenBitbucketDto.class);
        return validateCxFlowConfig(cxFlowConfigDto, (AccessTokenBitbucketDto)accessTokenBitbucketDto);

    }
    
    private List<OrganizationWebDto> getAndStoreOrganizations(AccessTokenBitbucketDto token) {
        ResponseEntity<BitbucketBaseListDto> response =
                restWrapper.sendBearerAuthRequest(URL_GET_WORKSPACES, HttpMethod.GET, null, null,
                        BitbucketBaseListDto.class, token.getAccessToken());
        List<BitbucketBase> organizationWebDtos = response.getBody().getElements();
        String tokenJson = AccessTokenManager.convertObjectToJson(token);
        List<OrgDto> orgDtos =
                Converter.convertToListOrg(tokenJson, organizationWebDtos, getBaseDbKey());
        dataStoreService.storeOrgs(orgDtos);

        return Converter.convertToListOrgWebDtos(organizationWebDtos);
    }

    private CxFlowConfigDto validateCxFlowConfig(CxFlowConfigDto cxFlowConfigDto, AccessTokenBitbucketDto token) {
        if(StringUtils.isAnyEmpty(cxFlowConfigDto.getScmAccessToken(), cxFlowConfigDto.getTeam(),
                                  cxFlowConfigDto.getCxgoToken())) {
            log.error("CxFlow configuration settings validation failure, missing data");
            throw new ScmException("CxFlow configuration settings validation failure, missing data");
        }
        try {
            restWrapper.sendBearerAuthRequest(URL_VALIDATE_TOKEN, HttpMethod.GET, null, null,
                                              CxFlowConfigDto.class,
                                              cxFlowConfigDto.getScmAccessToken());
            log.info("Bitbucket token validation passed successfully!");
        } catch (HttpClientErrorException ex){
            token = refreshToken(token);
            cxFlowConfigDto.setScmAccessToken(token.getAccessToken());
            log.info("Bitbucket refresh token process passed successfully!");
        }
        return cxFlowConfigDto;
    }

    private AccessTokenBitbucketDto refreshToken(AccessTokenBitbucketDto token) {
        token = sendRefreshTokenRequest(token.getRefreshToken());
        getAndStoreOrganizations(token);
        return token;
    }



    private AccessTokenBitbucketDto sendRefreshTokenRequest(String refreshToken) {
        return sendGenerateAccessTokenRequest(URL_AUTH_TOKEN, getAccessTokenHeaders(),
                                              getBodyRefreshAccessToken(refreshToken));
    }

    private WebhookBitbucketDto getRepositoryCxFlowWebhook(@NonNull String repoId, @NonNull String workspaceId,
                                                           @NonNull String accessToken){
        String path = String.format(URL_GET_WEBHOOKS, workspaceId, repoId);
        ResponseEntity<WebhookBitbucketListDto> response =  restWrapper.sendBearerAuthRequest(path, HttpMethod.GET,
                null, null,
                WebhookBitbucketListDto.class, accessToken);
        WebhookBitbucketListDto webhookDtos = Objects.requireNonNull(response.getBody());

        return (WebhookBitbucketDto)getActiveHook(webhookDtos.getElements());
    }
    
    private AccessTokenBitbucketDto generateAccessToken(String oAuthCode) {
        HttpHeaders headers = getAccessTokenHeaders();
        return sendGenerateAccessTokenRequest(URL_AUTH_TOKEN, headers, getBodyAccessToken(oAuthCode));
    }

    private HttpHeaders getAccessTokenHeaders() {
        ScmDto scmDto = dataStoreService.getScm(getBaseDbKey());
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(scmDto.getClientId(), scmDto.getClientSecret(), StandardCharsets.UTF_8);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }


    private AccessTokenBitbucketDto sendGenerateAccessTokenRequest(String path, HttpHeaders headers, MultiValueMap<String, String> body) {
        ResponseEntity<AccessTokenBitbucketDto> response = restWrapper.sendUrlEncodedPostRequest(path,body, headers, AccessTokenBitbucketDto.class);

        AccessTokenBitbucketDto accessTokenDto = response.getBody();
        if(!verifyAccessToken(accessTokenDto)){
            log.error(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
            throw new ScmException(RestWrapper.GENERATE_ACCESS_TOKEN_FAILURE);
        }
        return accessTokenDto;
    }

    private MultiValueMap<String, String> getBodyAccessToken(String oAuthCode) {

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();

        map.put("grant_type",  Collections.singletonList("authorization_code"));
        map.put("code", Collections.singletonList(oAuthCode));

        return map;
    }

    private MultiValueMap<String, String> getBodyRefreshAccessToken(String refreshToken) {

        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();

        map.put("grant_type",  Collections.singletonList("refresh_token"));
        map.put("refresh_token", Collections.singletonList(refreshToken));

        return map;
    }



    
    
}
