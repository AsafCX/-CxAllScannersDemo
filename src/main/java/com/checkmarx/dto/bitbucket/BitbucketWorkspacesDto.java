package com.checkmarx.dto.bitbucket;

import com.checkmarx.dto.IDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BitbucketWorkspacesDto {


    @JsonProperty("values")
    private List<Workspace> values;
    
    public List<Workspace> getWorkspaces(){
        return values;
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Workspace implements IDto {

        @JsonProperty("slug")
        private String slug;
        @JsonProperty("name")
        private String name;
        
        @Override
        public String getId() {
            return  slug;
        }
        
    }
    
}
