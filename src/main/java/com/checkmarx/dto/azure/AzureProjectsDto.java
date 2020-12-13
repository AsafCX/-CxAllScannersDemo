package com.checkmarx.dto.azure;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AzureProjectsDto {

    private int count;
    private List<RepoAzureDto> value;

    public List<RepoAzureDto> getProjectIds(){
        return value;
    }
    
    
}
