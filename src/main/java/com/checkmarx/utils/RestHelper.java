package com.checkmarx.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestHelper {

    public static final String SAVE_OAUTH_FAILURE = "Save OAuth token failure - Wasn't able to save in database via DataStore service";
    public static final String GENERATE_OAUTH_TOKEN_FAILURE = "OAuth token generation failure";

    private RestHelper() {
    }
}
