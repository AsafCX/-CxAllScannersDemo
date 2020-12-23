package com.checkmarx.dto.bitbucket;

import com.checkmarx.dto.IDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitbucketBase implements IDto {

    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("slug")
    private String id;
    @JsonProperty("name")
    private String name;
    
}
