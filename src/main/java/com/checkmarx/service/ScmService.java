package com.checkmarx.service;

import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.utils.RestWrapper;
import lombok.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

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
