package com.checkmarx.dto.gitlab;

import com.checkmarx.dto.IRepoDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RepoGitlabDto implements IRepoDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name_with_namespace")
    private String name;

    @JsonProperty("namespace")
    private Namespace namespace;

    private String webhookId;
    private boolean webHookEnabled;
    
    @Data
    public class Namespace{
        
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;
    }
}
