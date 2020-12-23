package com.checkmarx.dto.bitbucket;

import com.checkmarx.dto.AccessTokenDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccessTokenBitbucketDto extends AccessTokenDto {

    @JsonProperty("scopes")
    public String scope;
    @JsonProperty("expires_in")
    public Integer expiresIn;
    @JsonProperty("token_type")
    public String tokenType;
    @JsonProperty("state")
    public String state;
    @JsonProperty("refresh_token")
    public String refreshToken;



}