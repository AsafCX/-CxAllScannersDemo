package com.checkmarx.utils;

import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.OrgReposDto;
import com.checkmarx.dto.github.AccessTokenGithubDto;
import com.checkmarx.dto.github.OrgGithubDto;
import com.checkmarx.dto.github.RepoGithubDto;
import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.checkmarx.dto.web.OrgWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Converter {


    public static OrgReposDto convertToOrgRepoDto(ScmAccessTokenDto scmAccessTokenDto, List<RepoGithubDto> orgRepoGithubDtos) {
        return OrgReposDto.builder()
                        .scmUrl(scmAccessTokenDto.getScmUrl())
                        .orgName(scmAccessTokenDto.getOrgName())
                        .repoList(Converter.convertListToRepoDto(orgRepoGithubDtos))
                        .build();

    }

    public static List<RepoDto> convertListToRepoDto(List<RepoGithubDto> orgRepoGithubDtos) {
        List<RepoDto> repoDtos = new ArrayList<>();
        for (RepoGithubDto repoDto: orgRepoGithubDtos) {
            repoDtos.add(Converter.convertToRepoDto(repoDto));
        }
        return repoDtos;
    }

    public static RepoDto convertToRepoDto(RepoGithubDto repoDto) {
        return RepoDto.builder()
                .name(repoDto.getName())
                .isWebhookConfigured(repoDto.isWebHookEnabled())
                .webhookId(repoDto.getWebhookId())
                .build();
    }

    public static List<OrgWebDto> convertToListOrgWebDtos(List<OrgGithubDto> userOrgGithubDtos) {
        List<OrgWebDto> orgWebDtos = new ArrayList<>();
        for (OrgGithubDto orgGithubDto: userOrgGithubDtos) {
            orgWebDtos.add(Converter.convertToOrgWebDto(orgGithubDto));
        }
        return orgWebDtos;
    }

    public static OrgWebDto convertToOrgWebDto(OrgGithubDto orgGithubDto) {
        return OrgWebDto.builder().id(orgGithubDto.getLogin()).name(orgGithubDto.getLogin()).build();
    }

    public static List<RepoWebDto> convertToListRepoWebDto(List<RepoGithubDto> orgRepoGithubDtos) {
        List<RepoWebDto> repoWebDtos = new ArrayList<>();
        for (RepoGithubDto repoGithubDto: orgRepoGithubDtos) {
            repoWebDtos.add(Converter.convertRepoGithubDtoToRepoWebDto(repoGithubDto));
        }
        return repoWebDtos;
    }

    public static RepoWebDto convertRepoGithubDtoToRepoWebDto(RepoGithubDto repoGithubDto) {
        return RepoWebDto.builder()
                .id(repoGithubDto.getName())
                .name(repoGithubDto.getName())
                .webhookId(repoGithubDto.getWebhookId())
                .webhookEnabled(repoGithubDto.isWebHookEnabled())
                .build();
    }

    public static OrgPropertiesDto convertToCxFlowProperties(String scmUrl, String orgName,
                                                             OrgSettingsWebDto orgSettingsWebDto) {
        return OrgPropertiesDto.builder()
                .cxTeam(orgSettingsWebDto.getTeam())
                .cxGoToken(orgSettingsWebDto.getCxgoSecret())
                .orgName(orgName)
                .scmUrl(scmUrl)
                .build();
    }

    public static RepoWebDto convertRepoDtoToRepoWebDto(RepoDto repoDto) {
        return RepoWebDto.builder()
                .id(repoDto.getName())
                .name(repoDto.getName())
                .webhookId(repoDto.getWebhookId())
                .webhookEnabled(repoDto.isWebhookConfigured())
                .build();
    }

    public static OrgSettingsWebDto convertOrgProToOrgSettingsWebDto(OrgPropertiesDto orgPropertiesDto) {
        return OrgSettingsWebDto.builder()
                .team(orgPropertiesDto.getCxTeam())
                .cxgoSecret(orgPropertiesDto.getCxGoToken())
                .build();
    }

    public static List<ScmAccessTokenDto> convertToListOrgAccessToken(AccessTokenGithubDto accessToken, List<OrgGithubDto> userOrgGithubDtos) {
        List<ScmAccessTokenDto> scmAccessTokenDtos = new ArrayList<>();
        for (OrgGithubDto orgGithubDto: userOrgGithubDtos) {
            scmAccessTokenDtos.add(ScmAccessTokenDto.builder()
                                           .orgName(orgGithubDto.getLogin())
                                           .scmUrl("github.com")
                                           .accessToken(accessToken.getAccessToken())
                                           .tokenType(TokenType.ACCESS.getType())
                                           .build());
        }
        return scmAccessTokenDtos;
    }
}
