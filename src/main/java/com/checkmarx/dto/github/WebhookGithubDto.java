package com.checkmarx.dto.github;

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
public @Data class WebhookGithubDto {

    private String type;
    private String id;
    private String name;
    private Boolean active;
    public List<String> events;
    private Config config;

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

