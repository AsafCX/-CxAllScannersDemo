package com.checkmarx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    public static final String SEPARATOR = ";";
    
    private String id;

    public BaseDto merge(BaseDto inDto){
        return merge(inDto.id);
    }

    public BaseDto merge(String inId){
        if(StringUtils.isEmpty(this.id)){
            this.id = inId;
        }
        else if(inId != null && StringUtils.isNotEmpty(inId)) {
            this.id = this.id + SEPARATOR + inId;
        }
        return this;
    }
    
    
    public static List<String> splitId(String id){
        if(StringUtils.isEmpty(id)){
            return new LinkedList<>();
        }
        return Arrays.asList(id.split(SEPARATOR));
    }
}
