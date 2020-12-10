package com.checkmarx.dto.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrgDto {
    @JsonProperty("scm_url")
    private String scmUrl;
    @JsonProperty("org_identity")
    private String orgIdentity;
    @JsonProperty("org_name")
    private String orgName;
    private String accessToken;
    private String tokenType;
}