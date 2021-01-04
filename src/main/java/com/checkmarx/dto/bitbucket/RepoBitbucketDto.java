package com.checkmarx.dto.bitbucket;

import com.checkmarx.dto.IRepoDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
public class RepoBitbucketDto extends BitbucketBase implements IRepoDto {
    
    private String webhookId;
    private boolean webHookEnabled;
    
}
