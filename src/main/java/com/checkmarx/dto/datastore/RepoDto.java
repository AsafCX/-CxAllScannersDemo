package com.checkmarx.dto.datastore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepoDto {
    @JsonProperty("name")
    private String repoId;
    @JsonProperty("webhook_id")
    private String webhookId;
    @JsonProperty("is_webhook_configured")
    private boolean isWebhookConfigured;
}
