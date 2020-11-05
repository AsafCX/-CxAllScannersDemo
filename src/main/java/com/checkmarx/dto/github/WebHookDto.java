package com.checkmarx.dto.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "type",
        "id",
        "name",
        "active",
        "config"
})
@NoArgsConstructor
@AllArgsConstructor
public @Data class WebHookDto {

    @JsonProperty("type")
    private String type;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("active")
    private Boolean active;
    @JsonProperty("config")
    private Config config;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({
            "content_type",
            "insecure_ssl",
            "url"
    })
    @NoArgsConstructor
    @AllArgsConstructor
    public @Data class Config {

        @JsonProperty("content_type")
        private String contentType;
        @JsonProperty("insecure_ssl")
        private String insecureSsl;
        @JsonProperty("url")
        private String url;
    }
}

