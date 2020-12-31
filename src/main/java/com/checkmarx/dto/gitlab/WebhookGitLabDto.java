package com.checkmarx.dto.gitlab;

import com.checkmarx.dto.BaseDto;
import com.checkmarx.dto.IDto;
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
    private boolean push_events = true;
    private boolean merge_requests_events = true;

    @Override
    public boolean isPushOrPull(){ return push_events || merge_requests_events;}
    
    @Override
    public boolean isActive() {
        return true;
    }
}

