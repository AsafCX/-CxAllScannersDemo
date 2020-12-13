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
    

    public int getCount(){
        return count;
    }

    public List<AzureWebhookDto> getWebhooks(){
        return value;
    }
}
