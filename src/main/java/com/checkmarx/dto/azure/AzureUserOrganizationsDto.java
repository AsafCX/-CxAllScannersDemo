package com.checkmarx.dto.azure;

import com.checkmarx.dto.IDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AzureUserOrganizationsDto {

    private int count;
    private List<Organization> value;

    public List<Organization> getOrganizations(){
        return value;
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Organization implements IDto {

        private String accountId;
        private String accountUri;
        private String accountName;

        @Override
        public String getId() {
            return accountId;
        }

        @Override
        public String getName() {
            return accountName;
        }
    }
    
}
