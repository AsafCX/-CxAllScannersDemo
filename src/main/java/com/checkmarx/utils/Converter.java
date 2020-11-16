package com.checkmarx.utils;

import com.checkmarx.dto.datastore.CxFlowPropertiesDto;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmRepoDto;
import com.checkmarx.dto.github.OrgGithubDto;
import com.checkmarx.dto.github.RepoGithubDto;
import com.checkmarx.dto.web.CxGoWebDto;
import com.checkmarx.dto.web.OrgWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Converter {


    public static ScmRepoDto convertToSCMRepoDto(ScmAccessTokenDto scmAccessTokenDto, List<RepoGithubDto> orgRepoGithubDtos) {
        return ScmRepoDto.builder()
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
            orgWebDtos.add(Converter.convertToOrgGithubDto(orgGithubDto));
        }
        return orgWebDtos;
    }

    public static OrgWebDto convertToOrgGithubDto(OrgGithubDto orgGithubDto) {
        return OrgWebDto.builder().id(orgGithubDto.getLogin()).name(orgGithubDto.getLogin()).build();
    }

    public static List<RepoWebDto> convertToListRepoWebDto(List<RepoGithubDto> orgRepoGithubDtos) {
        List<RepoWebDto> repoWebDtos = new ArrayList<>();
        for (RepoGithubDto repoGithubDto: orgRepoGithubDtos) {
            repoWebDtos.add(Converter.convertToOrgGithubDto(repoGithubDto));
        }
        return repoWebDtos;
    }

    public static RepoWebDto convertToOrgGithubDto(RepoGithubDto repoGithubDto) {
        return RepoWebDto.builder()
                .id(repoGithubDto.getName())
                .name(repoGithubDto.getName())
                .webhookId(repoGithubDto.getWebhookId())
                .webhookEnabled(repoGithubDto.isWebHookEnabled())
                .build();
    }

    public static CxFlowPropertiesDto convertToCxFlowProperties(String scmUrl, String orgName,
                                                                CxGoWebDto cxGoWebDto) {
        return CxFlowPropertiesDto.builder()
                .cxTeam(cxGoWebDto.getTeam())
                .cxGoToken(cxGoWebDto.getCxgoSecret())
                .orgName(orgName)
                .scmUrl(scmUrl)
                .build();
    }
}
