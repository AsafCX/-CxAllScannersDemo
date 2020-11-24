package com.checkmarx.service;

import com.checkmarx.dto.datastore.*;
import lombok.NonNull;

import java.util.List;

public interface DataService {

    public void storeScmOrgsToken(@NonNull List<ScmAccessTokenDto> scmAccessTokenDtos);
    public ScmAccessTokenDto getSCMOrgToken(@NonNull String scmUrl, @NonNull String orgName);
    public void storeScm(@NonNull ScmDto scmDto);
    public ScmDto getScm(@NonNull String scmUrl);
    public void storeScmOrgRepos(@NonNull OrgReposDto orgReposDto);
    public List<RepoDto> getScmOrgRepos(@NonNull String scmUrl, @NonNull String orgName);
    public RepoDto getScmOrgRepo(@NonNull String scmUrl, @NonNull String orgName,
                                 @NonNull String repoName);
    public void updateScmOrgRepo(@NonNull OrgReposDto orgReposDto);
    public OrgPropertiesDto getScmOrgSettings(@NonNull String scmUrl, @NonNull String orgName);
    public void storeScmOrgSettings(@NonNull OrgPropertiesDto orgPropertiesDto);
}
