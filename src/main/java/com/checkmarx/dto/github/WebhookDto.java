package com.checkmarx.dto.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "type",
        "id",
        "name",
        "active",
        "events",
        "config"
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class WebhookDto {

    @JsonProperty("type")
    private String type;
    @JsonProperty("id")
    private Long id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("events")
    public List<String> events;
    @JsonProperty("config")
    private Config config;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "url",
            "content_type",
            "secret",
            "insecure_ssl"
    })
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public @Data
    static class Config {

        @JsonProperty("url")
        private String url;
        @JsonProperty("content_type")
        private String contentType;
        @JsonProperty("secret")
        private String secret;
        @JsonProperty("insecure_ssl")
        private String insecureSsl;

    }
}

