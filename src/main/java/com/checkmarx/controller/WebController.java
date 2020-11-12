package com.checkmarx.controller;

import com.checkmarx.dto.web.OrgWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.dto.web.ScmConfigDto;
import com.checkmarx.service.ScmService;
import com.checkmarx.utils.RestHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping
public class WebController {

    @Autowired
    ScmService scmService;

    @Autowired
    RestHelper restHelper;

    @Autowired
    ApplicationContext applicationContext;

    /**
     * Rest api used by FE application on start-up, Retrieve client id from DataStore and
     * scope from app properties file
     *
     * @param scmName Given Scm to handle
     *
     * @return ResponseEntity with status:200, Body: Github client id & scope
     */
    @GetMapping(value = "/{scmName}/config")
    public ResponseEntity getConfiguration(@PathVariable String scmName) {
        log.trace("getConfiguration: scmName={}", scmName);
        ScmConfigDto scmConfigDto = getScmService(scmName).getScmConfiguration();
        log.info("Return Scm: {} Configuration: {}",scmName, scmConfigDto);
        return ResponseEntity.ok(scmConfigDto);
    }

    /**
     * Rest api used to first create OAuth access token and retrieve all user organizations from given scm
     *
     * @param scmName Given Scm to handle
     * @param authCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return ResponseEntity with status:200, Body: list of all user organizations
     */
    @PostMapping(value = "/{scmName}/user/orgs")
    public ResponseEntity getOrganizations(@PathVariable String scmName,
                                           @RequestParam String authCode) {
        log.trace("getOrganizations: scmName={}, authCode={}", scmName, authCode);
        List<OrgWebDto> userOrgGithubDtos = getScmService(scmName).getOrganizations(authCode);
        log.info("Return Scm: {} Organizations: {}", scmName, userOrgGithubDtos);
        return ResponseEntity.ok(userOrgGithubDtos);
    }

    /**
     * Rest api used to get all user repositories from given scm
     *
     * @param scmName Given Scm to handle
     * @param userAccessToken access token using it for scm authorization header
     * @return ResponseEntity with https status:200, Body: list of all user repositories (private
     *         and public)
     */
    @GetMapping(value = "/{scmName}/user/repos")
    public ResponseEntity getUserRepositories(@PathVariable String scmName, @RequestHeader String userAccessToken) {
        log.trace("getUserRepositories: scmName={}",scmName);
        List<RepoWebDto> userRepoGithubDtos =
                getScmService(scmName).getUserRepositories(userAccessToken);
        log.info("Return Scm: {} user repositories: {}", scmName, userRepoGithubDtos);
        return ResponseEntity.ok(userRepoGithubDtos);
    }

    /**
     * Rest api used to get for specific organization all repositories (private and public)
     *
     * @param scmName Given Scm to handle
     * @param orgName organization name used to retrieve the relevant repositories
     * @return ResponseEntity with http status:200, Body: all organization repositories (public
     *         and private)
     */
    @GetMapping(value = "/{scmName}/orgs/{orgName}/repos")
    public ResponseEntity getOrganizationRepositories(@PathVariable String scmName,
                                                      @PathVariable String orgName) {
        log.trace("getOrganizationRepositories: scmName={}, orgName={}", scmName, orgName);
        List<RepoWebDto> orgRepoGithubDtos = getScmService(scmName).getScmOrgRepos(orgName);
        log.info("Return Scm: {} Organization: {} repositories: {}", scmName, orgName,
                 orgRepoGithubDtos);
        return ResponseEntity.ok(orgRepoGithubDtos);
    }

    @PostMapping(value = "/{scmName}/orgs/{orgName}/repos/{repoName}/webhooks")
    public ResponseEntity createWebhook(@PathVariable String scmName,
                                        @PathVariable String orgName,
                                        @PathVariable String repoName) {
        log.trace("createWebhook: scmName={}, orgName={}, repoName={}", scmName, orgName, repoName);
        getScmService(scmName).createWebhook(orgName, repoName);
        log.info("{} CXFlow Webhook created successfully!",repoName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{scmName}/orgs/{orgName}/repos/{repoName}/webhooks/{webhookId}")
    public ResponseEntity deleteWebhook(@PathVariable String scmName,
                                        @PathVariable String orgName,
                                        @PathVariable String repoName,
                                        @PathVariable String webhookId) {
        log.trace("deleteWebhook: scmName={}, orgName={}, repoName={}, webhookId={}",scmName, orgName,
                  repoName, webhookId);
        getScmService(scmName).deleteWebhook(orgName, repoName, webhookId);
        log.info("{} CXFlow Webhook removed successfully!",repoName);
        return ResponseEntity.ok().build();
    }

    private ScmService getScmService(String scmName) {
        return (ScmService) applicationContext.getBean(scmName);
    }
}
