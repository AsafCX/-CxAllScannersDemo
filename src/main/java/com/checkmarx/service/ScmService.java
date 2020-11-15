package com.checkmarx.service;

import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.web.OrgWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.dto.web.ScmConfigWebDto;
import lombok.NonNull;

import java.util.List;

public interface ScmService {

    public ScmConfigWebDto getScmConfiguration();
    public List<OrgWebDto> getOrganizations(@NonNull String authCode);
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgName);
    public RepoDto getScmOrgRepo(@NonNull String orgName, @NonNull String repoName);
    public String createWebhook(@NonNull String orgName, @NonNull String repoName);
    public void deleteWebhook(@NonNull String orgName, @NonNull String repoName, @NonNull String webhookId);
}
