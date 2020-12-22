package com.checkmarx.dto.azure;

import com.checkmarx.dto.AccessTokenDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccessTokenAzureDto extends AccessTokenDto {
    
    @JsonProperty("token_type")
    public String tokenType;
    @JsonProperty("refresh_token")
    public String refreshToken;
    @JsonProperty("scope")
    public String scope;
    @JsonProperty("expires_in")
    public Integer expiresIn;

}