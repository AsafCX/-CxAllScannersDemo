package com.checkmarx.dto.azure;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AzureEvent {

    UPDATE_PULL_REQEUST("git.pullrequest.updated", "%s/ado/pull"),
    CREATE_PULL_REQEUST("git.pullrequest.created","%s/ado/pull"),
    PUSH( "git.push","%s/ado/push");

    private String type;
    private String hookUrl;
    
}
