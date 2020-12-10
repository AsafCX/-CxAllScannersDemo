package com.checkmarx.dto.gitlab;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class WebhookGitLabDto  {

    private String id;
    private String url;
    @JsonProperty("project_id")
    private Boolean projectId;
  
}

