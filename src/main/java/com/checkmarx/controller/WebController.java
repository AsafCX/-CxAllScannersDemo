package com.checkmarx.controller;

import com.checkmarx.dto.web.OrgSettingsWebDto;

import com.checkmarx.dto.web.OrganizationWebDto;
import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.dto.web.ScmConfigWebDto;
import com.checkmarx.service.GenericScmService;
import com.checkmarx.service.ScmService;

import io.swagger.v3.oas.annotations.Operation;
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
    ApplicationContext applicationContext;
    
    @Autowired
    GenericScmService genericScmService;

    /**
     * @param scmType Given Scm to handle
     *
     * @return ResponseEntity with status:200, Body: Github client id & scope
     */
    @Operation(summary = "Rest api used by FE application on start-up, Retrieve client id from " +
            "DataStore and scope from app properties file")
    @GetMapping(value = "/{scmType}/config")
    public ResponseEntity getConfiguration(@PathVariable String scmType) {
        log.trace("getConfiguration: scmType={}", scmType);
        String baseUrl = getScmService(scmType).getBaseUrl();
        String scopes = getScmService(scmType).getScopes();
        ScmConfigWebDto scmConfigWebDto = genericScmService.getScmConfiguration(baseUrl,scopes);
        log.info("Return Scm: {} Configuration: {}", scmType, scmConfigWebDto);
        return ResponseEntity.ok(scmConfigWebDto);
    }

    /**
     * Rest api used to first create OAuth access token and retrieve all user organizations from given scm
     *
     * @param scmType Given Scm to handle
     * @param authCode given from FE application after first-step OAuth implementation passed
     *                  successfully, taken from request param "code", using it to create access token
     * @return ResponseEntity with status:200, Body: list of all user organizations
     */
    @Operation(summary = "Rest api used to create OAuth access token and retrieve all user " +
            "organizations from given scm")
    @PostMapping(value = "/{scmType}/user/orgs")
    public ResponseEntity getOrganizations(@PathVariable String scmType,
                                           @RequestParam String authCode) {
        log.trace("getOrganizations: scmType={}, authCode={}", scmType, authCode);
        List<OrganizationWebDto> userOrgGithubDtos = getScmService(scmType).getOrganizations(authCode);
        log.info("Return Scm: {} Organizations: {}", scmType, userOrgGithubDtos);
        return ResponseEntity.ok(userOrgGithubDtos);
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgName organization name used to retrieve the relevant repositories
     * @return ResponseEntity with http status:200, Body: all organization repositories (public
     *         and private)
     */
    @Operation(summary = "Rest api used to get for specific organization all repositories (private and public)")
    @GetMapping(value = "/{scmType}/orgs/{orgName}/repos")
    public ResponseEntity getOrganizationRepositories(@PathVariable String scmType,
                                                      @PathVariable String orgName) {
        log.trace("getOrganizationRepositories: scmType={}, orgName={}", scmType, orgName);
        List<RepoWebDto> orgRepoGithubDtos = getScmService(scmType).getScmOrgRepos(orgName);
        log.info("Return Scm: {} Organization: {} repositories: {}", scmType, orgName,
                 orgRepoGithubDtos);
        return ResponseEntity.ok(orgRepoGithubDtos);
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgName organization name
     * @param repoName repository name to create webhook
     * @return ResponseEntity with http status:200, Body: webhook id
     */
    @Operation(summary = "Rest api used to create webhook for given scm organization repository")
    @PostMapping(value = "/{scmType}/orgs/{orgName}/repos/{repoName}/webhooks")
    public ResponseEntity createWebhook(@PathVariable String scmType,
                                        @PathVariable String orgName,
                                        @PathVariable String repoName) {
        log.trace("createWebhook: scmType={}, orgName={}, repoName={}", scmType, orgName, repoName);
        String webhookId = getScmService(scmType).createWebhook(orgName, repoName);
        log.info("{} CXFlow Webhook created successfully!",repoName);
        return ResponseEntity.ok(webhookId);
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgName organization name
     * @param repoName repository name
     * @param webhookId webhook id to delete
     * @return ResponseEntity with http status:200
     */
    @Operation(summary = "Rest api used to delete webhook from given scm organization repository")
    @DeleteMapping(value = "/{scmType}/orgs/{orgName}/repos/{repoName}/webhooks/{webhookId}")
    public ResponseEntity deleteWebhook(@PathVariable String scmType,
                                        @PathVariable String orgName,
                                        @PathVariable String repoName,
                                        @PathVariable String webhookId) {
        log.trace("deleteWebhook: scmType={}, orgName={}, repoName={}, webhookId={}",scmType, orgName,
                  repoName, webhookId);
        getScmService(scmType).deleteWebhook(orgName, repoName, webhookId);
        log.info("{} CXFlow Webhook removed successfully!",repoName);
        return ResponseEntity.ok().build();
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgName organization name
     * @return ResponseEntity with http status:200, Body: organization settings
     */
    @Operation(summary = "Rest api used to retrieve scm organization settings")
    @GetMapping(value = "/{scmType}/orgs/{orgName}/settings")
    public ResponseEntity getOrgSettings(@PathVariable String scmType, @PathVariable String orgName) {
        log.trace("getOrgSettings: scmType={}, orgName={}", scmType, orgName);
        String baseUrl = getScmService(scmType).getBaseUrl();
        OrgSettingsWebDto orgSettingsWebDto = genericScmService.getOrgSettings(orgName,baseUrl);
        log.info("Return organization settings: {} for scm: {}, org: {}", orgSettingsWebDto, scmType,
                 orgName);
        return ResponseEntity.ok(orgSettingsWebDto);
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgName organization name
     * @return ResponseEntity with http status:200
     */
    @Operation(summary = "Rest api used to create/update scm organization settings")
    @PutMapping(value = "/{scmType}/orgs/{orgName}/settings")
    public ResponseEntity setOrgSettings(@PathVariable String scmType, @PathVariable String orgName,
                                  @RequestBody OrgSettingsWebDto orgSettingsWebDto) {
        log.trace("setOrgSettings: scmType={}, orgName={}, OrgSettingsWebDto={}", scmType, orgName,
                  orgSettingsWebDto);
        String baseUrl = getScmService(scmType).getBaseUrl();
        genericScmService.setOrgSettings(orgName, orgSettingsWebDto,baseUrl);
        log.info("{} organization settings saved successfully!", orgSettingsWebDto);
        return ResponseEntity.ok().build();
    }


    private ScmService getScmService(String scmName) {
        return (ScmService) applicationContext.getBean(scmName);
    }
}
