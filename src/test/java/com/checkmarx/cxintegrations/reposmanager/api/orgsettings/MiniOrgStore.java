package com.checkmarx.cxintegrations.reposmanager.api.orgsettings;

import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.service.GitHubService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MiniOrgStore {
    private final List<OrgPropertiesDto> orgs = new ArrayList<>();

    private final String existingScmForTestPurposes = (new GitHubService(null,null)).getBaseDbKey();

    public MiniOrgStore() {
        log.info("Created a {} instance.", this.getClass().getSimpleName());
    }

    public void addOrg(String orgId, String scmId) {
        log.info("Putting the {} organization into miniOrgStore for the {} SCM.", orgId, scmId);
        orgs.add(OrgPropertiesDto.builder()
                .orgIdentity(orgId)
                .scmUrl(scmId)
                .build());
    }

    public OrgPropertiesDto getOrganization(String orgId) {
        return orgs.stream()
                .filter(org -> org.getOrgIdentity().equals(orgId))
                .findFirst()
                .orElse(null);
    }

    public void clearScmOrgs(String scmId) {
        orgs.removeIf(org -> org.getScmUrl().equals(scmId));
    }

    public OrgPropertiesDto findOrg(String orgId, String scmId) {
        log.info("Looking for the '{}' organization of the '{}' SCM.", orgId, scmId);
        OrgPropertiesDto orgWithId = getOrganization(orgId);
        return Optional.ofNullable(orgWithId)
                .filter(org -> org.getScmUrl().equals(scmId))
                .orElse(null);
    }

    public boolean hasScm(String scmId) {
        return scmId.equals(existingScmForTestPurposes);
    }

    public void updateOrCreateOrg(OrgPropertiesDto org) {
        log.info("updateOrCreateOrg for the organization: {}", org);
        OrgPropertiesDto existingOrg = findOrg(org.getOrgIdentity(), org.getScmUrl());
        if (existingOrg != null) {
            log.info("Organization already exists.");
            // We don't care about the actual update here: remove+add is enough.
            orgs.remove(existingOrg);
        }
        orgs.add(org);
    }
}
