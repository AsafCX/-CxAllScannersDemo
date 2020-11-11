package com.checkmarx.service;

import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.SCMAccessTokenDto;
import com.checkmarx.dto.datastore.SCMDto;
import com.checkmarx.dto.github.OrganizationDto;
import com.checkmarx.dto.github.RepositoryDto;
import com.checkmarx.dto.web.ScmConfigDto;
import lombok.NonNull;

import java.util.List;

public interface ScmService {

    @NonNull
    public void storeScm(SCMDto scmDto);
    @NonNull
    public SCMDto getScm(String baseUrl);
    @NonNull
    public void storeSCMOrgToken(SCMAccessTokenDto scmAccessTokenDto);
    @NonNull
    public SCMAccessTokenDto getSCMOrgToken(String scmUrl,String orgName);
    @NonNull
    public List<OrganizationDto> getOrganizations(String oAuthCode);
    @NonNull
    public void storeSCMOrgRepos(SCMAccessTokenDto scmAccessTokenDto,
                                 List<RepositoryDto> orgRepositoryDtos);
    @NonNull
    public List<RepositoryDto> getSCMOrgRepos(String orgName);
    @NonNull
    public RepoDto getSCMOrgRepo(String githubUrl, String orgName, String repoName);
    @NonNull
    public void updateSCMOrgRepos(SCMAccessTokenDto scmAccessTokenDto,
                                  List<RepositoryDto> orgRepositoryDtos);
    @NonNull
    public void updateSCMOrgRepoWebhook(SCMAccessTokenDto scmAccessTokenDto, RepoDto repoDto);
    @NonNull
    public void createWebhook(String orgName, String repoName);
    @NonNull
    public void deleteWebhook(String orgName, String repoName);
    @NonNull
    public List<RepositoryDto> getUserRepositories(String userAccessToken);
    @NonNull
    public ScmConfigDto getScmConfiguration();
}
