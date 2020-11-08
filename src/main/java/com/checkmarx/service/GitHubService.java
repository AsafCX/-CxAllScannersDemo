package com.checkmarx.service;

import com.checkmarx.controller.DataStoreController;
import com.checkmarx.dto.RepoDto;
import com.checkmarx.dto.SCMAccessTokenDto;
import com.checkmarx.dto.SCMDto;
import com.checkmarx.dto.SCMRepoDto;
import com.checkmarx.dto.github.AccessTokenDto;
import com.checkmarx.dto.github.OrganizationDto;
import com.checkmarx.dto.github.RepositoryDto;
import com.checkmarx.utils.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
public class GitHubService {

    @Autowired
    DataStoreController dataStoreController;

    private Map<String, AccessTokenDto> synchronizedMap =
            Collections.synchronizedMap(new HashMap<String, AccessTokenDto>());


    public void addAccessToken(AccessTokenDto accesToken, List<OrganizationDto> orgsDto) {
        synchronized (synchronizedMap) {
            for (OrganizationDto orgDto : orgsDto) {
                if (synchronizedMap.containsKey(orgDto.getLogin())) {
                    synchronizedMap.remove(orgDto.getLogin());
                }
                synchronizedMap.put(orgDto.getLogin(), accesToken);
            }
        }
    }

    public AccessTokenDto getAccessToken(String orgName) {
        AccessTokenDto accessToken = new AccessTokenDto();
        synchronized (synchronizedMap) {
            if (synchronizedMap.containsKey(orgName)) {
                accessToken = synchronizedMap.get(orgName);
            }
        }
        return accessToken;
    }

    public void storeScm(SCMDto scmDto) {
        dataStoreController.storeScm(scmDto);
    }

    public void storeSCMOrgToken(SCMAccessTokenDto scmAccessTokenDto) {
        dataStoreController.storeSCMOrgToken(scmAccessTokenDto);
    }

    public SCMDto getScm(String githubUrl) {
        return dataStoreController.getScm(githubUrl);
    }

    public void storeSCMOrgRepos(SCMAccessTokenDto scmAccessTokenDto, List<RepositoryDto> orgRepositoryDtos) {
        SCMRepoDto scmRepoDto = Converter.convertToSCMRepoDto(scmAccessTokenDto, orgRepositoryDtos);
        dataStoreController.storeSCMOrgRepos(scmRepoDto);
    }

    public List<RepoDto> getSCMOrgRepos(String githubUrl, String orgName) {
        return dataStoreController.getSCMReposByOrg(githubUrl, orgName);
    }

    public RepoDto getSCMOrgRepo(String githubUrl, String orgName, String repoName) {
       return dataStoreController.getSCMOrgRepo(githubUrl, orgName, repoName);

    }
}
