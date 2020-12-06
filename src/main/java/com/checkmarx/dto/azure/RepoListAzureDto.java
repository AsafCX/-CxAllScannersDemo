package com.checkmarx.dto.azure;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RepoListAzureDto {

    @JsonProperty("value")
    private List<RepoAzureDto> value;
    @JsonProperty("count")
    private int count;
    
    public List<RepoAzureDto> getRepos(){
        return value;
    }
    
    public int getCount(){
        return count;
    }

  
}
