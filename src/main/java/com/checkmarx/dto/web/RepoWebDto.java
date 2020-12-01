package com.checkmarx.dto.web;

import com.checkmarx.dto.IDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public @Data class RepoWebDto implements IDto {

    private String id;
    private String name;
    private String webhookId;
    private boolean webhookEnabled;
}
