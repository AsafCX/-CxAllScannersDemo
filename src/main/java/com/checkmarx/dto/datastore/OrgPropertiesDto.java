package com.checkmarx.dto.datastore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "scm_url",
        "org_name",
        "cx_flow_url",
        "cx_go_token",
        "cx_team"
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrgPropertiesDto {
    @JsonProperty("scm_url")
    private String scmUrl;
    @JsonProperty("org_name")
    private String orgName;
    @JsonProperty("cx_flow_url")
    private String cxFlowUrl;
    @JsonProperty("cx_go_token")
    private String cxGoToken;
    @JsonProperty("cx_team")
    private String cxTeam;
}