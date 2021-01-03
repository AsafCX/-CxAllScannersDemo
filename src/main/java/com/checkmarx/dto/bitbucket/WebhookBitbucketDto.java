package com.checkmarx.dto.bitbucket;

import com.checkmarx.dto.IWebhookDto;
import com.checkmarx.dto.azure.AzureEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class WebhookBitbucketDto implements IWebhookDto {

    @JsonProperty("description")
    private String description;
    @JsonProperty("url")
    private String url;
    @JsonProperty("active")
    private boolean active;
    @JsonProperty("events")
    private List<String> events;
    @JsonProperty("type")
    private String type;
    @JsonProperty("uuid")
    private String uuid;
    @JsonIgnore
    private String id = null;
    
    public String getId() {
        
        if(id != null){
            return id;
        }
        if(uuid != null){
            id = uuid.replaceAll("\\{", "").replaceAll("}", "");
        }
        return id;
    }

    public String getName() {
        return uuid;
    }

    public boolean isPushOrPull() {

        return events != null
                && (events.contains(BitBucketEvent.CREATE_PULL_REQEUST.getType())
                || events.contains(BitBucketEvent.UPDATE_PULL_REQEUST.getType())
                || events.contains(BitBucketEvent.PUSH.getType()));
    }
}

