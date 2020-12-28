package com.checkmarx.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenInfoDto {
    public static final String FIELD_ACCESS_TOKEN = "access_token";
    public static final String FIELD_REFRESH_TOKEN = "refresh_token";

    @JsonProperty(FIELD_ACCESS_TOKEN)
    private String accessToken;

    @JsonProperty(FIELD_REFRESH_TOKEN)
    private String refreshToken;

    @JsonIgnore
    private ObjectNode additionalData;
}
