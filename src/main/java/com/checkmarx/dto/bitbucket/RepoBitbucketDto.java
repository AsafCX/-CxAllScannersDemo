package com.checkmarx.dto.bitbucket;

import com.checkmarx.dto.IRepoDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RepoBitbucketDto implements IRepoDto {

    @JsonProperty("slug")
    private String id;

    @JsonProperty("name")
    private String name;
    
    private String webhookId;
    private boolean webHookEnabled;
   
}
