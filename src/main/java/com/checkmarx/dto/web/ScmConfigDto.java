package com.checkmarx.dto.web;


import lombok.Builder;
import lombok.Data;

@Builder
public @Data class ScmConfigDto {

    private String clientId;
    private String scope;

}


