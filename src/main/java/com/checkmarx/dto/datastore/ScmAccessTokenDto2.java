package com.checkmarx.dto.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

// TODO: replace the existing ScmAccessTokenDto with this class when it becomes possible.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScmAccessTokenDto2 {
    Long id;

    @JsonProperty("access_token")
    String accessToken;
}
