package com.checkmarx.service;

import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.datastore.ScmRepoDto;
import com.checkmarx.dto.web.OrgWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.dto.web.ScmConfigDto;
import lombok.NonNull;

import java.util.List;

public interface ScmService {

    public ScmConfigDto getScmConfiguration();
    public ScmDto getScm(@NonNull String baseUrl);
    public void storeScmOrgToken(@NonNull ScmAccessTokenDto scmAccessTokenDto);
    public ScmAccessTokenDto getScmOrgToken(@NonNull String scmUrl, @NonNull String orgName);
    public List<OrgWebDto> getOrganizations(@NonNull String authCode);
    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgName);
    public RepoDto getScmOrgRepo(@NonNull String githubUrl, @NonNull String orgName,
                                 @NonNull String repoName);
    public void storeScmOrgRepos(@NonNull ScmRepoDto scmRepoDto);
    public void updateScmOrgRepos(@NonNull ScmRepoDto scmRepoDto);
    public String createWebhook(@NonNull String orgName, @NonNull String repoName);
    public void deleteWebhook(@NonNull String orgName, @NonNull String repoName, @NonNull String webhookId);
}
