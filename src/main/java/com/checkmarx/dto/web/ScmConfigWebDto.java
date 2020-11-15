package com.checkmarx.dto.web;


import lombok.Builder;
import lombok.Data;

@Builder
public @Data class ScmConfigWebDto {

    private String clientId;
    private String scope;

}


