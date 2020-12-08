package com.checkmarx.dto.azure;

import com.checkmarx.dto.IDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WebhookListAzureDto {
    
    @JsonProperty("count")
    private int count;
    @JsonProperty("value")
    private List<AzureWebhookDto> value;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class AzureWebhookDto implements IDto {
        
        @JsonProperty("id")
        private String id;
        @JsonProperty("url")
        private String url;
        @JsonProperty("status")
        private String status;
        @JsonProperty("eventType")
        private String eventType;
        @JsonProperty("publisherInputs")
        private PublisherInputs publisherInputs;
        @JsonProperty("consumerInputs")
        private ConsumerInputs consumerInputs;
        
        private static final String PUSH_EVENT = "git.push";
        private static final String PULL_EVENT = "git.pullrequest";
        private static final String ENABLED = "enabled";


        @JsonInclude(JsonInclude.Include.NON_NULL)
        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class ConsumerInputs{

            @JsonProperty("url")
            private String url;
        }

        
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class PublisherInputs{
            
            @JsonProperty("projectId")
            private String projectId;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @AllArgsConstructor
        @NoArgsConstructor
        @Data
        public static class CustomerInputs{

            @JsonProperty("branch")
            private String branch;
            @JsonProperty("projectId")
            private String projectId;
        }
        
        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return id;
        }

        public boolean isPushOrPull(){
            
            return eventType!=null && ENABLED.equals(status) && (eventType.contains(PULL_EVENT) || eventType.contains(PULL_EVENT));
        }

        public String getProjectId(){return publisherInputs.projectId;}

    }

    public int getCount(){
        return count;
    }

    public List<AzureWebhookDto> getWebhooks(){
        return value;
    }
}
