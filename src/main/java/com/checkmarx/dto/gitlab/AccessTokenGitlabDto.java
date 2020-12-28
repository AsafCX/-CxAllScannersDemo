package com.checkmarx.dto.gitlab;

import com.checkmarx.dto.AccessTokenDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AccessTokenGitlabDto extends AccessTokenDto {
    
    @JsonProperty("token_type")
    public String tokenType;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("scope")
    public String scope;

    @JsonProperty("created_at")
    public Integer createdAt;
}