package com.checkmarx.dto.datastore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenInfoDto {
    public static final String FIELD_ACCESS_TOKEN = "access_token";
    public static final String FIELD_REFRESH_TOKEN = "refresh_token";

    private long id;

    @JsonProperty(FIELD_ACCESS_TOKEN)
    private String accessToken;

    @JsonProperty(FIELD_REFRESH_TOKEN)
    private String refreshToken;

    @JsonIgnore
    private final Map<String, String> additionalData = new HashMap<>();
}
