package com.checkmarx.controller;

import com.checkmarx.dto.datastore.*;
import com.checkmarx.dto.web.CxGoWebDto;
import lombok.NonNull;

import java.util.List;

public interface DataController {

    public void storeScmOrgToken(@NonNull ScmAccessTokenDto scmAccessToken);
    public ScmAccessTokenDto getSCMOrgToken(@NonNull String scmUrl, @NonNull String orgName);
    public void storeScm(@NonNull ScmDto scmDto);
    public ScmDto getScm(@NonNull String scmUrl);
    public void storeScmOrgRepos(@NonNull ScmRepoDto scmRepoDto);
    public List<RepoDto> getScmOrgRepos(@NonNull String scmUrl, @NonNull String orgName);
    public RepoDto getScmOrgRepo(@NonNull String scmUrl, @NonNull String orgName,
                                 @NonNull String repoName);
    public void updateScmOrgRepo(@NonNull ScmRepoDto scmRepoDto);
    public CxGoWebDto getScmOrgCxGo(@NonNull String scmUrl,@NonNull String orgName);
    public void setScmOrgCxGo(@NonNull CxFlowPropertiesDto cxFlowPropertiesDto);
}
