package com.checkmarx.dto.gitlab;

import com.checkmarx.dto.IWebhookDto;
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
public @Data class WebhookGitLabDto  implements IWebhookDto {

    private String id;
    private String url;
    @JsonProperty("project_id")
    private Boolean projectId;
    @JsonProperty("push_events")
    private boolean pushEvents = true;
    @JsonProperty("merge_requests_events")
    private boolean mergeRequestsEvents = true;

    @Override
    public boolean isPushOrPull(){ return pushEvents || mergeRequestsEvents;}
    
    @Override
    public boolean isActive() {
        return true;
    }
}

