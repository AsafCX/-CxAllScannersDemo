package com.checkmarx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public @Data class BaseDto {

    private static final String SEPARATOR = "cxint;";
     
    private String id;

    public BaseDto(String id1, String id2){
        id = id1 + SEPARATOR + id2;
    }
    
    public BaseDto join(BaseDto inDto){
        return join(inDto.id);
    }

    public BaseDto join(String inId){
        return join(inId, SEPARATOR);
    }
    
    private BaseDto join(String inId, String separator) {
        if(StringUtils.isEmpty(this.id)){
            this.id = inId;
        }
        else if(StringUtils.isNotEmpty(inId)) {
            this.id = this.id + separator + inId;
        }
        return this;
    }


    public List<String> split(){
        
        if(StringUtils.isEmpty(id)){
            return new LinkedList<>();
        }
        return Arrays.asList(id.split(SEPARATOR));
    }
    
}
