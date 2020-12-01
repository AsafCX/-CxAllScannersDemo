package com.checkmarx.utils;


import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.IDto;
import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.dto.datastore.OrgReposDto;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;


import com.checkmarx.dto.IRepoDto;
import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Converter {

    private Converter(){}
    
    public static OrgReposDto convertToOrgRepoDto(ScmAccessTokenDto scmAccessTokenDto, List<? extends IRepoDto> repoDtos) {
        return OrgReposDto.builder()
                .scmUrl(scmAccessTokenDto.getScmUrl())
                .orgIdentity(scmAccessTokenDto.getOrgIdentity())
                .repoList(Converter.convertListToRepoDto(repoDtos))
                .build();

    }

    public static List<RepoDto> convertListToRepoDto(List<? extends IRepoDto>  dtoList) {
        List<RepoDto> repoDtos = new ArrayList<>();
        for (IRepoDto repoDto: dtoList) {
            repoDtos.add(Converter.convertToDataStoreDto(repoDto));
        }
        return repoDtos;
    }

    public static RepoDto convertToDataStoreDto(IRepoDto repoDto) {
        return RepoDto.builder()
                .repoIdentity(repoDto.getId())
                .isWebhookConfigured(repoDto.isWebHookEnabled())
                .webhookId(repoDto.getWebhookId())
                .build();
    }
    
    public static List<OrganizationWebDto> convertToListOrgWebDtos(List<? extends IDto> userOrgGithubDtos) {
        List<OrganizationWebDto> orgWebDtos = new ArrayList<>();
        for (IDto orgGithubDto: userOrgGithubDtos) {
            orgWebDtos.add(Converter.convertToOrgWebDto(orgGithubDto));
        }
        return orgWebDtos;
    }

    public static OrganizationWebDto convertToOrgWebDto(IDto iDto) {
        return OrganizationWebDto.builder().id(iDto.getId()).name(iDto.getName()).build();
    }


    public static List<RepoWebDto> convertToListRepoWebDto(List<? extends IRepoDto>  repoDtos) {
        List<RepoWebDto> repoWebDtos = new ArrayList<>();
        for (IRepoDto repoDto : repoDtos) {
            repoWebDtos.add(Converter.convertRepoDtoToRepoWebDto(repoDto));
        }
        return repoWebDtos;
    }
    


    public static RepoWebDto convertRepoDtoToRepoWebDto(IRepoDto repoDto) {
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
    

    public static List<ScmAccessTokenDto> convertToListOrgAccessToken(AccessTokenDto accessToken, List<? extends IDto> organizationWebDtos, String scmUrl) {
        List<ScmAccessTokenDto> scmAccessTokenDtos = new ArrayList<>();
        for (IDto orgDto: organizationWebDtos) {
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
