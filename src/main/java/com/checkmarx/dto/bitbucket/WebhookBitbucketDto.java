package com.checkmarx.dto.bitbucket;

import com.checkmarx.dto.IWebhookDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class WebhookBitbucketDto implements IWebhookDto {

    private String description;
    private String url;
    private boolean active;
    private List<String> events;
    private String type;
    private String uuid;

    @JsonIgnore
    private String id;
    
    public String getId() {
        
        if(id != null){
            return id;
        }
        if(uuid != null){
            id = uuid.replace("{", "").replace("}", "");
        }
        return id;
    }

    public String getName() {
        return uuid;
    }

    @Override
    public boolean isPushOrPull() {

        return events != null
                && (events.contains(BitBucketEvent.CREATE_PULL_REQEUST.getType())
                || events.contains(BitBucketEvent.UPDATE_PULL_REQEUST.getType())
                || events.contains(BitBucketEvent.PUSH.getType()));
    }
}

