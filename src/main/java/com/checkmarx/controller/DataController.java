package com.checkmarx.controller;

import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.datastore.ScmRepoDto;
import lombok.NonNull;

import java.util.List;

public interface DataController {

    public void storeScmOrgToken(@NonNull ScmAccessTokenDto scmAccessToken);
    public ScmAccessTokenDto getSCMOrgToken(@NonNull String scmUrl, @NonNull String orgName);
    public void storeScm(@NonNull ScmDto scmDto);
    public ScmDto getScm(@NonNull String scmUrl);
    public void storeScmOrgRepos(@NonNull ScmRepoDto scmRepoDto);
    public List<RepoDto> getScmOrgRepos(@NonNull String baseUrl, @NonNull String orgName);
    public RepoDto getScmOrgRepo(@NonNull String baseUrl, @NonNull String orgName,
                                 @NonNull String repoName);
    public void updateScmOrgRepo(@NonNull ScmRepoDto scmRepoDto);
}
