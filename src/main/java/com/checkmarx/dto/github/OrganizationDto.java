package com.checkmarx.dto.github;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "login",
        "id"
})
public @Data class OrganizationDto implements Serializable {

    @JsonProperty("login")
    private String login;
    @JsonProperty("id")
    private Integer id;

    public OrganizationDto(OrganizationDto newOrganizationDto) {
        this.login = newOrganizationDto.login;
        this.id = newOrganizationDto.id;
    }

    public OrganizationDto() {
    }
}