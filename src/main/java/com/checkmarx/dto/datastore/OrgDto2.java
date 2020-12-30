package com.checkmarx.dto.datastore;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrgDto2 {
    private String orgIdentity;
    private long tokenId;
}
