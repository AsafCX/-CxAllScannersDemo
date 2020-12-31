package com.checkmarx.dto.github;

import com.checkmarx.dto.IWebhookDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class WebhookGithubDto implements IWebhookDto {

    private String type;
    private String id;
    private String name;
    private boolean active;
    private List<String> events;
    private Config config;

    @Override
    public String getUrl() {
        return config.getUrl();
    }
    
    public void setUrl(String url) {
        if(config == null){
            config = new Config();
        }
        config.setUrl(url);
    }

    @Override
    public boolean isPushOrPull() {
        return events != null
                && (events.contains(GithubEvent.PULL_REQEUST.getType())
                || events.contains(GithubEvent.PUSH.getType()));

    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {

        private String url;
        @JsonProperty("content_type")
        private String contentType;
        private String secret;
        @JsonProperty("insecure_ssl")
        private String insecureSsl;

    }
}

