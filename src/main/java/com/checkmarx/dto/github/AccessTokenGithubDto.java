package com.checkmarx.dto.github;


import com.checkmarx.dto.AccessTokenDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public @Data class AccessTokenGithubDto extends AccessTokenDto {

    @JsonProperty("scope")
    private String scope;
    @JsonProperty("token_type")
    private String type;
}
