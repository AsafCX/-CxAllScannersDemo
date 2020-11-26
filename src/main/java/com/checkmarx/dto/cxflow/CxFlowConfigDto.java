package com.checkmarx.dto.cxflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public @Data class CxFlowConfigDto {

    private String team;
    private String cxgoSecret;
    private String scmAccessToken;

    @Override
    public String toString() {
        return "CxFlowConfigDto{" +
                "team='" + team + '\'' +
                ", cxgoSecret='**********" + '\'' +
                ", scmAccessToken='**********" + '\'' +
                '}';
    }
}