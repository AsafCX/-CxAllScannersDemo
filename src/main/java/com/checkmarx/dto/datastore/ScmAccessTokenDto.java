package com.checkmarx.dto.datastore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class ScmAccessTokenDto {
    @JsonProperty("scm_url")
    private String scmUrl;
    @JsonProperty("org_identity")
    private String orgIdentity;
    private String accessToken;
    private String tokenType;

}
