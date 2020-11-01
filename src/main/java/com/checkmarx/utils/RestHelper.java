package com.checkmarx.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestHelper {

    public static final String SAVE_ACCESS_TOKEN_FAILURE = "Save access token failure - Wasn't able to save in database via DataStore service";
    public static final String GENERATE_ACCESS_TOKEN_FAILURE = "Access token generation failure";
    public static final String ACCESS_TOKEN_MISSING = "Access token is missing, please reconnect";

    private RestHelper() {
    }
}
