package com.checkmarx.utils;


import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.dto.datastore.OrgReposDto;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;

import com.checkmarx.dto.github.OrganizationGithubDto;

import com.checkmarx.dto.github.RepoGithubDto;
import com.checkmarx.dto.gitlab.RepoGitlabDto;
import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Converter {

    private Converter(){}

    public static OrgReposDto convertToOrgGithubRepoDto(ScmAccessTokenDto scmAccessTokenDto, List<RepoGithubDto> orgRepoGithubDtos) {
        return OrgReposDto.builder()
                .scmUrl(scmAccessTokenDto.getScmUrl())
                .orgIdentity(scmAccessTokenDto.getOrgIdentity())
                .repoList(Converter.convertListToRepoGithubDto(orgRepoGithubDtos))
                .build();

    }
    public static OrgReposDto convertToOrgGitlabRepoDto(ScmAccessTokenDto scmAccessTokenDto, List<RepoGitlabDto> repoDtos) {
        return OrgReposDto.builder()
                .scmUrl(scmAccessTokenDto.getScmUrl())
                .orgIdentity(scmAccessTokenDto.getOrgIdentity())
                .repoList(Converter.convertListToRepoGitlabDto(repoDtos))
                .build();

    }
    public static List<RepoDto> convertListToRepoGithubDto(List<RepoGithubDto> dtoList) {
        List<RepoDto> repoDtos = new ArrayList<>();
        for (RepoGithubDto repoDto: dtoList) {
            repoDtos.add(Converter.convertToDataStoreDtoGithub(repoDto));
        }
        return repoDtos;
    }

    public static List<RepoDto> convertListToRepoGitlabDto(List<RepoGitlabDto> dtoList) {
        List<RepoDto> repoDtos = new ArrayList<>();
        for (RepoGitlabDto repoDto: dtoList) {
            repoDtos.add(Converter.convertToDataStoreDtoGitlab(repoDto));
        }
        return repoDtos;
    }

    
    public static RepoDto convertToDataStoreDtoGithub(RepoGithubDto repoDto) {
        return RepoDto.builder()
                .repoIdentity(repoDto.getName())
                .isWebhookConfigured(repoDto.isWebHookEnabled())
                .webhookId(repoDto.getWebhookId())
                .build();
    }

    public static RepoDto convertToDataStoreDtoGitlab(RepoGitlabDto repoDto) {
        return RepoDto.builder()
                .repoIdentity(repoDto.getId())
                .isWebhookConfigured(repoDto.isWebHookEnabled())
                .webhookId(repoDto.getWebhookId())
                .build();
    }
    
    public static List<OrganizationWebDto> convertToListOrgWebDtos(List<OrganizationGithubDto> userOrgGithubDtos) {
        List<OrganizationWebDto> orgWebDtos = new ArrayList<>();
        for (OrganizationGithubDto orgGithubDto: userOrgGithubDtos) {
            orgWebDtos.add(Converter.convertToOrgWebDto(orgGithubDto));
        }
        return orgWebDtos;
    }

    public static OrganizationWebDto convertToOrgWebDto(OrganizationGithubDto orgGithubDto) {
        return OrganizationWebDto.builder().id(orgGithubDto.getName()).name(orgGithubDto.getName()).build();
    }

    public static List<RepoWebDto> convertToListRepoGithubWebDto(List<RepoGithubDto> repoDtos) {
        List<RepoWebDto> repoWebDtos = new ArrayList<>();
        for (RepoGithubDto repoDto : repoDtos) {
            repoWebDtos.add(Converter.convertRepoDtoToRepoGithubWebDto(repoDto));
        }
        return repoWebDtos;
    }


    public static List<RepoWebDto> convertToListRepoGitlabWebDto(List<RepoGitlabDto> repoDtos) {
        List<RepoWebDto> repoWebDtos = new ArrayList<>();
        for (RepoGitlabDto repoDto : repoDtos) {
            repoWebDtos.add(Converter.convertRepoDtoToRepoGitlabWebDto(repoDto));
        }
        return repoWebDtos;
    }
    
    public static RepoWebDto convertRepoDtoToRepoGithubWebDto(RepoGithubDto repoDto) {
        return RepoWebDto.builder()
                .id(repoDto.getName())
                .name(repoDto.getName())
                .webhookId(repoDto.getWebhookId())
                .webhookEnabled(repoDto.isWebHookEnabled())
                .build();
    }

    public static RepoWebDto convertRepoDtoToRepoGitlabWebDto(RepoGitlabDto repoDto) {
        return RepoWebDto.builder()
                .id(repoDto.getId())
                .name(repoDto.getName())
                .webhookId(repoDto.getWebhookId())
                .webhookEnabled(repoDto.isWebHookEnabled())
                .build();
    }
    public static OrgPropertiesDto convertToCxFlowProperties(String scmUrl, String orgName,
                                                             OrgSettingsWebDto orgSettingsWebDto) {
        return OrgPropertiesDto.builder()
                .cxTeam(orgSettingsWebDto.getTeam())
                .cxGoToken(orgSettingsWebDto.getCxgoSecret())
                .orgIdentity(orgName)
                .scmUrl(scmUrl)
                .build();
    }
    

    public static OrgSettingsWebDto convertOrgProToOrgSettingsWebDto(OrgPropertiesDto orgPropertiesDto) {
        String cxTeam = "";
        String cxGoToken = "";
        if (orgPropertiesDto.getCxTeam() != null){
            cxTeam = orgPropertiesDto.getCxTeam();
        }
        if (orgPropertiesDto.getCxGoToken() != null){
            cxGoToken = orgPropertiesDto.getCxGoToken();
        }
        return OrgSettingsWebDto.builder()
                .team(cxTeam)
                .cxgoSecret(cxGoToken)
                .build();
    }

    public static List<ScmAccessTokenDto> convertToListGithubOrgAccessToken(AccessTokenDto accessToken, List<OrganizationGithubDto> userOrgGithubDtos, String scmUrl) {
        List<ScmAccessTokenDto> scmAccessTokenDtos = new ArrayList<>();
        for (OrganizationGithubDto orgGithubDto: userOrgGithubDtos) {
            scmAccessTokenDtos.add(ScmAccessTokenDto.builder()
                                           .orgIdentity(orgGithubDto.getName())
                                            .scmUrl(scmUrl)
                                           .accessToken(accessToken.getAccessToken())
                                           .tokenType(TokenType.ACCESS.getType())
                                           .build());
        }
        return scmAccessTokenDtos;
    }

    public static List<ScmAccessTokenDto> convertToListGitlabOrgAccessToken(AccessTokenDto accessToken, List<OrganizationWebDto> organizationWebDtos, String scmUrl) {
        List<ScmAccessTokenDto> scmAccessTokenDtos = new ArrayList<>();
        for (OrganizationWebDto orgDto: organizationWebDtos) {
            scmAccessTokenDtos.add(ScmAccessTokenDto.builder()
                    .orgIdentity(orgDto.getId())
                    .scmUrl(scmUrl)
                    .accessToken(accessToken.getAccessToken())
                    .tokenType(TokenType.ACCESS.getType())
                    .build());
        }
        return scmAccessTokenDtos;
    }
}
