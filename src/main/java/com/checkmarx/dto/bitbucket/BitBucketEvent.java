package com.checkmarx.dto.bitbucket;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BitBucketEvent {

    UPDATE_PULL_MERGED("pullrequest:fulfilled"),
    UPDATE_PULL_REQEUST("pullrequest:updated"),
    CREATE_PULL_REQEUST("pullrequest:created"),
    PUSH( "repo:push");

    private String type;
    
}
