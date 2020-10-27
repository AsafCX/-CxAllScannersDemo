package com.checkmarx.dto.github;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "login",
        "id",
        "node_id",
        "url",
        "repos_url",
        "events_url",
        "hooks_url",
        "issues_url",
        "members_url",
        "public_members_url",
        "avatar_url",
        "description"
})
public class OrganizationDto implements Serializable {

    @JsonProperty("login")
    private String login;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("node_id")
    private String nodeId;
    @JsonProperty("url")
    private String url;
    @JsonProperty("repos_url")
    private String reposUrl;
    @JsonProperty("events_url")
    private String eventsUrl;
    @JsonProperty("hooks_url")
    private String hooksUrl;
    @JsonProperty("issues_url")
    private String issuesUrl;
    @JsonProperty("members_url")
    private String membersUrl;
    @JsonProperty("public_members_url")
    private String publicMembersUrl;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("description")
    private String description;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public OrganizationDto(OrganizationDto newOrganizationDto) {
        this.login = newOrganizationDto.login;
        this.id = newOrganizationDto.id;
        this.nodeId = newOrganizationDto.nodeId;
        this.url = newOrganizationDto.url;
        this.reposUrl = newOrganizationDto.reposUrl;
        this.eventsUrl = newOrganizationDto.eventsUrl;
        this.hooksUrl = newOrganizationDto.hooksUrl;
        this.issuesUrl = newOrganizationDto.issuesUrl;
        this.membersUrl = newOrganizationDto.membersUrl;
        this.publicMembersUrl = newOrganizationDto.publicMembersUrl;
        this.avatarUrl = newOrganizationDto.avatarUrl;
        this.description = newOrganizationDto.description;
        this.additionalProperties = newOrganizationDto.additionalProperties;
    }

    public OrganizationDto() {
    }

    @JsonProperty("login")
    public String getLogin() {
        return login;
    }

    @JsonProperty("login")
    public void setLogin(String login) {
        this.login = login;
    }

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("node_id")
    public String getNodeId() {
        return nodeId;
    }

    @JsonProperty("node_id")
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("repos_url")
    public String getReposUrl() {
        return reposUrl;
    }

    @JsonProperty("repos_url")
    public void setReposUrl(String reposUrl) {
        this.reposUrl = reposUrl;
    }

    @JsonProperty("events_url")
    public String getEventsUrl() {
        return eventsUrl;
    }

    @JsonProperty("events_url")
    public void setEventsUrl(String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }

    @JsonProperty("hooks_url")
    public String getHooksUrl() {
        return hooksUrl;
    }

    @JsonProperty("hooks_url")
    public void setHooksUrl(String hooksUrl) {
        this.hooksUrl = hooksUrl;
    }

    @JsonProperty("issues_url")
    public String getIssuesUrl() {
        return issuesUrl;
    }

    @JsonProperty("issues_url")
    public void setIssuesUrl(String issuesUrl) {
        this.issuesUrl = issuesUrl;
    }

    @JsonProperty("members_url")
    public String getMembersUrl() {
        return membersUrl;
    }

    @JsonProperty("members_url")
    public void setMembersUrl(String membersUrl) {
        this.membersUrl = membersUrl;
    }

    @JsonProperty("public_members_url")
    public String getPublicMembersUrl() {
        return publicMembersUrl;
    }

    @JsonProperty("public_members_url")
    public void setPublicMembersUrl(String publicMembersUrl) {
        this.publicMembersUrl = publicMembersUrl;
    }

    @JsonProperty("avatar_url")
    public String getAvatarUrl() {
        return avatarUrl;
    }

    @JsonProperty("avatar_url")
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}