package com.checkmarx.controller;

import com.checkmarx.dto.BaseDto;
import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.web.*;
import com.checkmarx.service.ConfigurationService;
import com.checkmarx.service.ScmService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Pattern;
import java.util.List;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
public class WebController {

    private final ApplicationContext applicationContext;
    
    private final ConfigurationService genericScmService;

    //no special character regex validation
    private static final String VALIDATION_REGEX = "^[^`~!@#$%^&*+={}:;<>?๐฿]*$";

    /**
     * @param scmType Given Scm to handle
     *
     * @return ResponseEntity with status:200, Body: Github client id & scope
     */
    @Operation(summary = "Rest api used by FE application on start-up, Retrieve client id from " +
            "DataStore and scope from app properties file")
    @GetMapping(value = "/{scmType}/config")
    public ResponseEntity<ScmConfigWebDto> getConfiguration(@PathVariable String scmType) {
        log.trace("getConfiguration: scmType={}", scmType);
        String baseUrl = getScmService(scmType).getBaseDbKey();
        String scopes = getScmService(scmType).getScopes();
        ScmConfigWebDto scmConfigWebDto = genericScmService.getScmConfiguration(baseUrl,scopes);
        log.info("Return Scm: {} Configuration: {}", scmType, scmConfigWebDto);
        return ResponseEntity.ok(scmConfigWebDto);
    }

    /**
     * Rest api used to first create OAuth access token and retrieve all user organizations from given scm
     *
     * @param scmType  Given Scm to handle
     * @param authCode given from FE application after first-step OAuth implementation passed
     *                 successfully, taken from request param "code", using it to create access token
     * @return ResponseEntity with status:200, Body: list of all user organizations
     */
    @Operation(summary = "Rest api used to create OAuth access token and retrieve all user " +
            "organizations from given scm")
    @PostMapping(value = "/{scmType}/user/orgs")
    public ResponseEntity<List<OrganizationWebDto>> getOrganizations(@PathVariable String scmType,
                                                                     @RequestParam String authCode) {
        log.trace("getOrganizations: scmType={}, authCode={}", scmType, authCode);
        List<OrganizationWebDto> organizationWebDtos = getScmService(scmType).getOrganizations(authCode);
        log.info("Return Scm: {} Organizations: {}", scmType, organizationWebDtos);
        return ResponseEntity.ok(organizationWebDtos);
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgId organization name used to retrieve the relevant repositories
     * @return ResponseEntity with http status:200, Body: all organization repositories (public
     *         and private)
     */
    @Operation(summary = "Rest api used to get for specific organization all repositories (private and public)")
    @GetMapping(value = "/{scmType}/orgs/{orgId}/repos")
    public ResponseEntity<List<RepoWebDto>> getOrganizationRepositories(@PathVariable String scmType,
                                                                        @PathVariable @Pattern(regexp = VALIDATION_REGEX) String orgId) {
        log.trace("getOrganizationRepositories: scmType={}, orgId={}", scmType, orgId);
        List<RepoWebDto> repoWebDtos = getScmService(scmType).getScmOrgRepos(orgId);
        log.info("Return Scm: {} Organization: {} repositories: {}", scmType, orgId,
                 repoWebDtos);
        return ResponseEntity.ok(repoWebDtos);
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgId organization name
     * @param repoId repository name to create webhook
     * @return ResponseEntity with http status:200, Body: webhook id
     */
    @Operation(summary = "Rest api used to create webhook for given scm organization repository")
    @PostMapping(value = "/{scmType}/orgs/{orgId}/repos/{repoId}/webhooks")
    public ResponseEntity<BaseDto> createWebhook(@PathVariable String scmType,
                                        @PathVariable @Pattern(regexp = VALIDATION_REGEX) String orgId,
                                        @PathVariable @Pattern(regexp = VALIDATION_REGEX) String repoId) {
        log.trace("createWebhook: scmType={}, orgId={}, repoName={}", scmType, orgId, repoId);
        BaseDto webhookId = getScmService(scmType).createWebhook(orgId, repoId);
        log.info("{} CXFlow Webhook created successfully!",repoId);
        return ResponseEntity.ok(webhookId);
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgId organization name
     * @param repoId repository name
     * @param webhookId webhook id to delete
     * @return ResponseEntity with http status:200
     */
    @Operation(summary = "Rest api used to delete webhook from given scm organization repository")
    @DeleteMapping(value = "/{scmType}/orgs/{orgId}/repos/{repoId}/webhooks/{webhookId}")
    public ResponseEntity deleteWebhook(@PathVariable String scmType,
                                        @PathVariable @Pattern(regexp = VALIDATION_REGEX) String orgId,
                                        @PathVariable @Pattern(regexp = VALIDATION_REGEX) String repoId,
                                        @PathVariable @Pattern(regexp = VALIDATION_REGEX) String webhookId) {
        log.trace("deleteWebhook: scmType={}, repoId={}, repoId={}, webhookId={}",scmType, orgId,
                repoId, webhookId);
        getScmService(scmType).deleteWebhook(orgId, repoId, webhookId);
        log.info("{} CXFlow Webhook removed successfully!",repoId);
        return ResponseEntity.ok().build();
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgId organization name
     * @return ResponseEntity with http status:200, Body: organization settings
     */
    @Operation(summary = "Rest api used to retrieve scm organization settings")
    @GetMapping(value = "/{scmType}/orgs/{orgId}/settings")
    public ResponseEntity<OrgSettingsWebDto> getOrgSettings(@PathVariable String scmType,
                                                            @PathVariable @Pattern(regexp = VALIDATION_REGEX) String orgId) {
        log.trace("getOrgSettings: scmType={}, orgId={}", scmType, orgId);
        String baseUrl = getScmService(scmType).getBaseDbKey();
        OrgSettingsWebDto orgSettingsWebDto = genericScmService.getOrgSettings(orgId,baseUrl);
        log.info("Return organization settings: {} for scm: {}, org: {}", orgSettingsWebDto, scmType,
                 orgId);
        return ResponseEntity.ok(orgSettingsWebDto);
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgId organization name
     * @return ResponseEntity with http status:200
     */
    @Operation(summary = "Rest api used to create/update scm organization settings")
    @PutMapping(value = "/{scmType}/orgs/{orgId}/settings")
    public ResponseEntity<Object> setOrgSettings(@PathVariable String scmType, @PathVariable @Pattern(regexp = VALIDATION_REGEX) String orgId,
                                  @RequestBody OrgSettingsWebDto orgSettingsWebDto) {
        log.trace("setOrgSettings: scmType={}, orgId={}, OrgSettingsWebDto={}", scmType, orgId,
                  orgSettingsWebDto);
        String baseUrl = getScmService(scmType).getBaseDbKey();
        genericScmService.setOrgSettings(orgId, orgSettingsWebDto, baseUrl);
        log.info("{} organization settings saved successfully!", orgSettingsWebDto);
        return ResponseEntity.ok().build();
    }

    /**
     * @param scmType Given Scm to handle
     * @param orgId organization name
     * @return ResponseEntity with http status:200 body: CxFlow Configuration including CxGo
     * token, team and Scm access token
     */
    @Operation(summary = "Rest api used by CxFlow app - Get CxFlow org settings and token")
    @GetMapping(value = "/{scmType}/orgs/{orgId}/tenantConfig")
    public ResponseEntity<CxFlowConfigDto> getCxFlowConfiguration(@PathVariable String scmType,
                                                                  @PathVariable @Pattern(regexp = VALIDATION_REGEX) String orgId) {
        log.trace("getCxFlowConfiguration: scmType={}, orgId={}", scmType, orgId);
        CxFlowConfigDto cxFlowConfigDto = getScmService(scmType).getCxFlowConfiguration(orgId);
        log.info("Return CxFlow organization: {} settings: {}", orgId, cxFlowConfigDto);
        return ResponseEntity.ok(cxFlowConfigDto);
    }

    private ScmService getScmService(String scmName) {
        return (ScmService) applicationContext.getBean(scmName);
    }

}
