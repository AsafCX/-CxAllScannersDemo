package com.checkmarx.dto.github;


import lombok.Data;

public @Data class GitHubConfigDto {

    private String clientId;
    private  String scope;

    public GitHubConfigDto(String clientId, String scope) {
        this.clientId = clientId;
        this.scope = scope;
    }
}


