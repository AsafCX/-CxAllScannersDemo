package com.checkmarx.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccessTokenGitlabDto {

    @JsonProperty("access_token")
    public String accessToken;
    @JsonProperty("token_type")
    public String tokenType;
    @JsonProperty("refresh_token")
    public String refreshToken;
    @JsonProperty("scope")
    public String scope;
    @JsonProperty("created_at")
    public Integer createdAt;

}