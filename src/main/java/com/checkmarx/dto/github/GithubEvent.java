package com.checkmarx.dto.github;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
@Getter
public enum GithubEvent {
    
    PULL_REQUEST("pull_request"),
    PUSH( "push");

    private String type;
    
    public static List<String> getAllEventsList(){
        return Arrays.asList(PULL_REQUEST.type,PUSH.type);
    }
}
