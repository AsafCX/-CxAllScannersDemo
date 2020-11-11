package com.checkmarx.controller;

import com.checkmarx.dto.github.OrganizationDto;
import com.checkmarx.dto.github.RepositoryDto;
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
@RequestMapping()
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
     * @return ResponseEntity with status:200, Body: Github client id & scope
     */
    @GetMapping(value = "/{scmName}/config")
    public ResponseEntity getConfiguration(@PathVariable String scmName) {
        log.trace("getScmConfig: scmName={}", scmName);
        ScmConfigDto scmConfigDto = getScmService(scmName).getScmConfiguration();
        log.info("Return Scm: {} Configuration: {}",scmName, scmConfigDto);
        return ResponseEntity.ok(scmConfigDto);
    }

    /**
     * Rest api used to first create OAuth access token and retrieve all user organizations from given scm
     *
     * @param oAuthCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return ResponseEntity with status:200, Body: list of all user organizations
     */
    @PostMapping(value = "/{scmName}/user/orgs")
    public ResponseEntity getOrganizations(@RequestParam(name = "code") String oAuthCode,
                                           @PathVariable String scmName) {
        log.trace("getOrganizations: scmName={},code={}", scmName, oAuthCode);
        List<OrganizationDto> userOrganizationDtos = getScmService(scmName).getOrganizations(oAuthCode);
        log.info("Return Scm: {} Organizations: {}", scmName, userOrganizationDtos);
        return ResponseEntity.ok(userOrganizationDtos);
    }

    /**
     * Rest api used to get all user repositories from given scm
     *
     * @param userAccessToken access token using it for scm authorization header
     * @return ResponseEntity with https status:200, Body: list of all user repositories (private
     *         and public)
     */
    @GetMapping(value = "/{scmName}/user/repos")
    public ResponseEntity getUserRepositories(
            @RequestHeader("UserAccessToken") String userAccessToken, @PathVariable String scmName) {
        log.trace("getUserRepositories: scmName={},UserAccessToken={}",scmName, userAccessToken);
        List<RepositoryDto> userRepositoryDtos =
                getScmService(scmName).getUserRepositories(userAccessToken);
        log.info("Return Scm: {} user repositories: {}", scmName, userRepositoryDtos);
        return ResponseEntity.ok(userRepositoryDtos);
    }

    /**
     * Rest api used to get for specific organization all repositories (private and public)
     *
     * @param orgName organization name used to retrieve the relevant repositories
     * @return ResponseEntity with http status:200, Body: all organization repositories (public
     *         and private)
     */
    @GetMapping(value = "/{scmName}/orgs/{orgName}/repos")
    public ResponseEntity getOrganizationRepositories(@PathVariable String orgName,
                                                      @PathVariable String scmName) {
        log.trace("getOrganizationRepositories: orgName={}", orgName);
        List<RepositoryDto> orgRepositoryDtos = getScmService(scmName).getSCMOrgRepos(orgName);
        log.info("Return Scm: {} Organization: {} repositories: {}", scmName, orgName, orgRepositoryDtos);
        return ResponseEntity.ok(orgRepositoryDtos);
    }

    @PostMapping(value = "/{scmName}/orgs/{orgName}/repos/{repoName}/webhook")
    public ResponseEntity createWebhook(@PathVariable String orgName, @PathVariable String repoName,
                                        @PathVariable String scmName) {
        log.trace("createWebhook: orgName={}, repoName={}", orgName, repoName);
        getScmService(scmName).createWebhook(orgName, repoName);
        log.info("{} CXFlow Webhook created successfully!",repoName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{scmName}/orgs/{orgName}/repos/{repoName}/webhook")
    public ResponseEntity deleteWebhook(@PathVariable String orgName,
                                        @PathVariable String repoName, @PathVariable String scmName) {
        log.trace("deleteWebhook: orgName={}, repoName={}", orgName, repoName);
        getScmService(scmName).deleteWebhook(orgName, repoName);
        log.info("{} CXFlow Webhook removed successfully!",repoName);
        return ResponseEntity.ok().build();
    }

    private ScmService getScmService(String scmName) {
        return (ScmService) applicationContext.getBean(scmName);
    }
}
