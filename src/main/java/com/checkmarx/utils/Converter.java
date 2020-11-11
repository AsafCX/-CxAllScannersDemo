package com.checkmarx.utils;

import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.SCMAccessTokenDto;
import com.checkmarx.dto.datastore.SCMRepoDto;
import com.checkmarx.dto.github.RepositoryDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Converter {


    public static SCMRepoDto convertToSCMRepoDto(SCMAccessTokenDto scmAccessTokenDto, List<RepositoryDto> orgRepositoryDtos) {
        return SCMRepoDto.builder()
                        .scmUrl(scmAccessTokenDto.getScmUrl())
                        .orgName(scmAccessTokenDto.getOrgName())
                        .repoList(Converter.convertListToRepoDto(orgRepositoryDtos))
                        .build();

    }

    private static List<RepoDto> convertListToRepoDto(List<RepositoryDto> orgRepositoryDtos) {
        List<RepoDto> repoDtos = new ArrayList<>();
        for (RepositoryDto repoDto: orgRepositoryDtos) {
            repoDtos.add(Converter.convertToRepoDto(repoDto));
        }
        return repoDtos;
    }

    private static RepoDto convertToRepoDto(RepositoryDto repoDto) {
        return RepoDto.builder()
                .name(repoDto.getName())
                .isWebhookConfigured(repoDto.isWebHookEnabled())
                .webhookId(repoDto.getWebhookId())
                .build();
    }
}
