package com.checkmarx.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AzureWebhookDto {

    @JsonProperty("id")
    private String id;
    @JsonProperty("url")
    private String url;
    @JsonProperty("status")
    private String status;
    private String publisherId;
    @JsonProperty("eventType")
    private String eventType;
    private String consumerActionId;
    private String consumerId;
    private String resourceVersion;
    @JsonProperty("publisherInputs")
    private PublisherInputs publisherInputs;
    @JsonProperty("consumerInputs")
    private ConsumerInputs consumerInputs;

    private Integer scope;



    private static final String ENABLED = "enabled";
    public static final String PROJECT_NAME = "Any repository on project";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ConsumerInputs {

        private String httpHeaders;
        @JsonProperty("url")
        private String url;
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class CustomerInputs {

        @JsonProperty("branch")
        private String branch;
        @JsonProperty("projectId")
        private String projectId;
    }

    public String getHookId() {
        return id;
    }

    public boolean isPushOrPull() {

        return eventType != null && ENABLED.equals(status)
                && (eventType.equals(AzureEvent.CREATE_PULL_REQEUST.getType())
                || eventType.equals(AzureEvent.UPDATE_PULL_REQEUST.getType())
                || eventType.equals(AzureEvent.PUSH.getType()));
    }

    public String getProjectId() {
        return publisherInputs.getProjectId();
    }

    
    public String getRepositoryId() {
        return publisherInputs.getRepository();
    }


}