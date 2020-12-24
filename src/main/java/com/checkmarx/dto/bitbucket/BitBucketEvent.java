package com.checkmarx.dto.bitbucket;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum BitBucketEvent {

    UPDATE_PULL_MERGED("pullrequest:fulfilled"),
    UPDATE_PULL_REQEUST("pullrequest:updated"),
    CREATE_PULL_REQEUST("pullrequest:created"),
    PUSH( "repo:push");

    private String type;
    
    public static List<String> getAllEventsList(){
        return Arrays.asList(UPDATE_PULL_MERGED.type,
                UPDATE_PULL_REQEUST.type,CREATE_PULL_REQEUST.type,PUSH.type);
    }
}
