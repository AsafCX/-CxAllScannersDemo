package com.checkmarx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "name",
        "is_webhook_configured"
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepoDto {
    @JsonProperty("name")
    private String name;
    @JsonProperty("is_webhook_configured")
    private boolean isWebhookConfigured;
}
