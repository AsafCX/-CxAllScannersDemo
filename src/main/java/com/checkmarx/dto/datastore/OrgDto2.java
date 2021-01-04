package com.checkmarx.dto.datastore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrgDto2 {
    private String orgIdentity;
    private long tokenId;

    /**
     * Convenience property that is helpful in data transformations but is not sent to the DataStore.
     */
    @JsonIgnore
    private String name;
}
