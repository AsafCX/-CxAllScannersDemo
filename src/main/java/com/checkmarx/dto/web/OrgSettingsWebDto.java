package com.checkmarx.dto.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "team",
        "cxgoSecret"
})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public @Data class OrgSettingsWebDto {

    @JsonProperty("team")
    private String team;
    @JsonProperty("cxgoSecret")
    private String cxgoSecret;
}
