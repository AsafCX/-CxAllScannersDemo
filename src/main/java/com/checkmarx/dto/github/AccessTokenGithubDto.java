package com.checkmarx.dto.github;


import com.checkmarx.dto.AccessTokenDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;


@EqualsAndHashCode(callSuper = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccessTokenGithubDto extends AccessTokenDto {
    private String scope;

    @JsonProperty("token_type")
    private String type;
}
