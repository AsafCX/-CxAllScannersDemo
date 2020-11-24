package com.checkmarx.service;

import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import lombok.NonNull;

import java.util.List;

public interface ScmService {


    List<OrganizationWebDto> getOrganizations(@NonNull String authCode);
    List<RepoWebDto> getScmOrgRepos(@NonNull String orgId);
    String createWebhook(@NonNull String orgId, @NonNull String repoId);
    void deleteWebhook(@NonNull String orgId, @NonNull String repoId,
                              @NonNull String webhookId);
    CxFlowConfigDto validateCxFlowConfiguration(@NonNull CxFlowConfigDto cxFlowConfigDto);
    String getBaseUrl();
    String getScopes();
}
