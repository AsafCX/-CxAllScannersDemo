package com.checkmarx.dto.github;

import com.checkmarx.dto.IDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public @Data class OrganizationGithubDto implements IDto {

    @JsonProperty("login")
    private String name;

    @Override
    public String getId() {
        return name;
    }
}