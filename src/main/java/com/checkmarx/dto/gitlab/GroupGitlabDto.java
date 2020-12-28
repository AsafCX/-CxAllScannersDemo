package com.checkmarx.dto.gitlab;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupGitlabDto {
    // GitLab group response contains many more properties, but this is what we need for now.
    private String path;
    private String name;
}
