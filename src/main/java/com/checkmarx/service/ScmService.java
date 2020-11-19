package com.checkmarx.service;

import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.dto.web.ScmConfigWebDto;
import lombok.NonNull;

import java.util.List;

public interface ScmService {


    List<OrganizationWebDto> getOrganizations(@NonNull String authCode);
    List<RepoWebDto> getScmOrgRepos(@NonNull String orgName);
    String createWebhook(@NonNull String orgName, @NonNull String repoName);
    void deleteWebhook(@NonNull String orgName, @NonNull String repoName,
                              @NonNull String webhookId);

    String getBaseUrl();
    String getScopes();
}
