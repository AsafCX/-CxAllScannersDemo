package com.checkmarx.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestHelper {

    public static final String urlPatternGetUserOrganizations = "https://api.github.com/user/orgs";
    public static final String urlPatternGetUserRepositories = "https://api.github.com/user/repos";
    public static final String urlPatternGetOrgRepositories = "https://api.github.com/orgs/%s/repos?type=all";
    public static final String githubTokenPattern = "token %s";

    private RestHelper() {
    }
}
