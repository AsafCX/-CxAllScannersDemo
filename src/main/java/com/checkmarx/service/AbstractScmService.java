package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.*;

import com.checkmarx.dto.cxflow.CxFlowConfigDto;

import com.checkmarx.dto.datastore.OrgPropertiesDto;

import com.checkmarx.utils.AccessTokenManager;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractScmService {
    
    protected final RestWrapper restWrapper;
    
    protected final DataService dataStoreService;
    
    @Value("${redirect.url}")
    private String redirectUrl;

    @Value("${cxflow.webhook.url}")
    private String cxFlowUrl;
    
    
    /**
     * verifyAccessToken method used to verify access token creation, Currently checks if access
     * token created(not null or empty) without GitHub validation
     *
     * @param accessToken access token generated before using GitHub api, Gives access to relevant
     *                  GitHub data
     * @return true if verification passed successfully
     */
    protected boolean verifyAccessToken(AccessTokenDto accessToken) {
        return accessToken != null && StringUtils.isNotEmpty(accessToken.getAccessToken());
    }

    public String getCxFlowUrl() {
        return trimNonEmptyString("Cxflow URL", cxFlowUrl);

    }

    public String getRedirectUrl() {
        return trimNonEmptyString("Redirect URL", redirectUrl);
    }

    protected static String trimNonEmptyString(String strName, String strValue){
        if(StringUtils.isEmpty(strValue)){
            throw new ScmException("Empty " + strName);
        }
        return strValue.trim();
    }

    protected void setWebhookDetails(IRepoDto repoDto, IWebhookDto webhookDto) {
        if(webhookDto != null) {
            repoDto.setWebHookEnabled(true);
            repoDto.setWebhookId(webhookDto.getId());
        } else {
            repoDto.setWebHookEnabled(false);
        }
    }

    protected void validateWebhookDto(IWebhookDto webhookGithubDto) {
        if(webhookGithubDto == null || StringUtils.isEmpty(webhookGithubDto.getId())){
            log.error(RestWrapper.WEBHOOK_CREATE_FAILURE);
            throw new ScmException(RestWrapper.WEBHOOK_CREATE_FAILURE);
        }
    }

    protected CxFlowConfigDto getOrganizationSettings(String organizationId, String token) {
        OrgPropertiesDto orgPropertiesDto = dataStoreService.getScmOrgSettings(getBaseDbKey(),
                organizationId);
        return CxFlowConfigDto.builder()
                .team(orgPropertiesDto.getCxTeam())
                .cxgoToken(orgPropertiesDto.getCxGoToken())
                .scmAccessToken(token)
                .build();
    }
    
    public abstract String getBaseDbKey();

    protected IWebhookDto getActiveHook(List<? extends IWebhookDto> webhookDtos) {
        for (IWebhookDto webhookDto : webhookDtos) {
            if (webhookDto != null  && webhookDto.getUrl().equals(getCxFlowUrl()) && webhookDto.isActive() && webhookDto.isPushOrPull())
                return webhookDto;
        }
        return null;
    }


    protected void deleteWebhook(@NonNull String orgId, @NonNull String repoId, String deleteUrl, Class<?> type) {
        
        AccessTokenManager accessTokenWrapper = new AccessTokenManager(getBaseDbKey(), orgId, dataStoreService);
        
        try {
            restWrapper.sendBearerAuthRequest(deleteUrl, HttpMethod.DELETE, null, null,
                    type,
                    accessTokenWrapper.getAccessTokenStr());
            
        } catch (HttpClientErrorException ex){
            if(ex.getStatusCode().equals(HttpStatus.NOT_FOUND)){
                log.error("Webhook not found: {}", ex.getMessage());
                throw new ScmException(RestWrapper.WEBHOOK_DELETE_FAILURE);
            }
            throw new ScmException(RestWrapper.GENERAL_RUNTIME_EXCEPTION);
        }

        dataStoreService.updateWebhook(repoId, accessTokenWrapper.getDbDto(), null, false);
    }
    
}
