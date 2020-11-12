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

    @NonNull
    public ScmDto getScm(String baseUrl);
    @NonNull
    public void storeScmOrgToken(ScmAccessTokenDto scmAccessTokenDto);
    @NonNull
    public ScmAccessTokenDto getScmOrgToken(String scmUrl, String orgName);
    @NonNull
    public List<OrgWebDto> getOrganizations(String authCode);
    @NonNull
    public List<RepoWebDto> getScmOrgRepos(String orgName);
    @NonNull
    public RepoDto getScmOrgRepo(String githubUrl, String orgName, String repoName);
    @NonNull
    public void createWebhook(String orgName, String repoName);
    @NonNull
    public void deleteWebhook(String orgName, String repoName, String webhookId);
    @NonNull
    public List<RepoWebDto> getUserRepositories(String userAccessToken);
    @NonNull
    public ScmConfigDto getScmConfiguration();
}
