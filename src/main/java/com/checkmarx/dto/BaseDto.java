package com.checkmarx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public @Data class BaseDto {

    public static final String SEPARATOR = ";";
    
    @JsonProperty("id")
    private String id;

    public BaseDto merge(BaseDto toMerge){
        this.id = id + SEPARATOR + toMerge.id;
        return this;
    }
    

    public static List<String> splitId(String id){
        return Arrays.asList(id.split(SEPARATOR));
    }
}
