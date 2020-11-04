package com.checkmarx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "scm_url",
        "org_name",
        "accessToken",
        "tokenType"
})
public @Data class SCMAccessTokenDto {
    @JsonProperty("scm_url")
    private String scmUrl;
    @JsonProperty("org_name")
    private String orgName;
    @JsonProperty("accessToken")
    private String accessToken;
    @JsonProperty("tokenType")
    private String tokenType;

    public SCMAccessTokenDto(String scmUrl, String orgName, String accessToken,
                             String tokenType) {
        this.scmUrl = scmUrl;
        this.orgName = orgName;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }
}