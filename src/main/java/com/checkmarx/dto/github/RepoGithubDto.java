package com.checkmarx.dto.github;

import com.checkmarx.dto.IDto;
import com.checkmarx.dto.IRepoDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public @Data class RepoGithubDto implements IRepoDto {

    private String name;
    private String webhookId;
    private boolean webHookEnabled;

    @Override
    public String getId() {
        return name;
    }

    @Override
    public void setId(String id) {
        name = id;
    }
}