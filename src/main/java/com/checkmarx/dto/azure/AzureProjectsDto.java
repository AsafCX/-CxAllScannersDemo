package com.checkmarx.dto.azure;

import com.checkmarx.dto.BaseDto;
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
public class AzureProjectsDto {

    private int count;
    private List<BaseDto> value;

    public List<BaseDto> getProjectIds(){
        return value;
    }
    
    
}
