package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.IRepoDto;
import com.checkmarx.dto.IWebhookDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractScmService {
    private static final String MISSING_DATA = "CxFlow configuration settings validation failure: missing data.";

    protected final RestWrapper restWrapper;

    protected final DataService dataStoreService;

    protected final AccessTokenService tokenService;

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

    // TODO: remove after the migrations.
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
        TokenInfoDto tokenInfo = tokenService.getTokenInfo(getBaseDbKey(), orgId);

        try {
            restWrapper.sendBearerAuthRequest(deleteUrl, HttpMethod.DELETE, null, null,
                    type,
                    tokenInfo.getAccessToken());

        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.error("Webhook not found: {}", ex.getMessage());
                throw new ScmException(RestWrapper.WEBHOOK_DELETE_FAILURE);
            }
            throw new ScmException(RestWrapper.GENERAL_RUNTIME_EXCEPTION);
        }

        RepoDto repo = RepoDto.builder()
                .repoIdentity(repoId)
                .isWebhookConfigured(false)
                .webhookId(null)
                .build();
        dataStoreService.updateRepo2(getBaseDbKey(), orgId, repo);
    }

    protected static void setWebhookRelatedFields(IWebhookDto webhook, RepoDto target) {
        if (webhook != null) {
            target.setWebhookConfigured(true);
            target.setWebhookId(webhook.getId());
        } else {
            target.setWebhookConfigured(false);
        }
    }

    protected static List<RepoWebDto> getReposForWebClient(OrgReposDto reposForDataStore) {
        return reposForDataStore.getRepoList()
                .stream()
                .map(toRepoForWebClient())
                .collect(Collectors.toList());
    }

    protected void storeNewWebhook(String orgId, String repoId, IWebhookDto webhook) {
        RepoDto updateRepoRequest = RepoDto.builder()
                .isWebhookConfigured(true)
                .repoIdentity(repoId)
                .webhookId(webhook.getId())
                .build();

        dataStoreService.updateRepo2(getBaseDbKey(), orgId, updateRepoRequest);
    }

    private static Function<RepoDto, RepoWebDto> toRepoForWebClient() {
        return repo -> RepoWebDto.builder()
                .id(repo.getRepoIdentity())
                .name(repo.getName())
                .webhookEnabled(repo.isWebhookConfigured())
                .webhookId(repo.getWebhookId())
                .build();
    }

    protected static void validateFieldsArePresent(CxFlowConfigDto cxFlowConfigDto) {
        if (StringUtils.isAnyEmpty(cxFlowConfigDto.getScmAccessToken(),
                cxFlowConfigDto.getTeam(),
                cxFlowConfigDto.getCxgoToken())) {
            log.error(MISSING_DATA);
            throw new ScmException(MISSING_DATA);
        }
    }
}
