package com.checkmarx.service;

import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.web.OrgWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.dto.web.ScmConfigDto;
import lombok.NonNull;

import java.util.List;

public interface ScmService {


    public ScmDto getScm(@NonNull String baseUrl);

    public void storeScmOrgToken(@NonNull ScmAccessTokenDto scmAccessTokenDto);

    public ScmAccessTokenDto getScmOrgToken(@NonNull String scmUrl, @NonNull String orgName);

    public List<OrgWebDto> getOrganizations(@NonNull String authCode);

    public List<RepoWebDto> getScmOrgRepos(@NonNull String orgName);

    public RepoDto getScmOrgRepo(@NonNull String githubUrl, @NonNull String orgName,
                                 @NonNull String repoName);

    public String createWebhook(@NonNull String orgName, @NonNull String repoName);

    public void deleteWebhook(@NonNull String orgName, @NonNull String repoName, @NonNull String webhookId);

    public List<RepoWebDto> getUserRepositories(@NonNull String userAccessToken);

    public ScmConfigDto getScmConfiguration();
}
