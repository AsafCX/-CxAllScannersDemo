package com.checkmarx.dto.bitbucket;

import com.checkmarx.dto.IRepoDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RepoBitbucketDto extends BitbucketBase implements IRepoDto {
    
    private String webhookId;
    private boolean webHookEnabled;
    
}
