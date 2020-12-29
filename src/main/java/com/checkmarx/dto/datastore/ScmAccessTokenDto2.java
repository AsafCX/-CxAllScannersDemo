package com.checkmarx.dto.datastore;

import lombok.*;

// TODO: replace the existing ScmAccessTokenDto with this class when it becomes possible.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScmAccessTokenDto2 {
    long id;

    String accessToken;
}
