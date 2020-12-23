package com.checkmarx.cxintegrations.reposmanager;

import com.checkmarx.cxintegrations.reposmanager.api.orgsettings.MiniOrgStore;
import com.checkmarx.dto.datastore.OrgPropertiesDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Intercepts organization-related DataStore requests.
 * Returns mock responses based on in-memory storage.
 */
@Slf4j
@RequiredArgsConstructor
public class OrgRequestInterceptor implements Answer<Object> {
    private final MiniOrgStore miniOrgStore;
    private final QueryStringInterceptor queryStringInterceptor;

    @Override
    public Object answer(InvocationOnMock invocation) {
        log.info("Intercepted a request to data store: {}", invocation);
        ResponseEntity<?> fakeResponse = null;
        HttpMethod method = invocation.getArgument(1);
        if (method == HttpMethod.GET) {
            fakeResponse = tryFindOrgInStore(invocation);
        } else if (method == HttpMethod.POST) {
            fakeResponse = trySaveOrgToStore(invocation);
        } else {
            log.info("Intercepted something else, returning {}.", fakeResponse);
        }
        return fakeResponse;
    }

    private ResponseEntity<?> trySaveOrgToStore(InvocationOnMock invocation) {
        log.info("Trying to save an organization to store: {}", invocation);
        OrgPropertiesDto org = getOrgFromRequest(invocation);
        ResponseEntity<?> savingResult;
        if (miniOrgStore.hasScm(org.getScmUrl())) {
            miniOrgStore.updateOrCreateOrg(org);
            savingResult = ResponseEntity.ok().build();
        } else {
            log.info("SCM not found, throwing an exception.");
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
        }
        return savingResult;
    }

    private OrgPropertiesDto getOrgFromRequest(InvocationOnMock invocation) {
        HttpEntity<OrgPropertiesDto> request = invocation.getArgument(2);
        OrgPropertiesDto org = request.getBody();
        if (org == null) {
            throw new RuntimeException("Organization is somehow null in the request body.");
        }
        return org;
    }

    private ResponseEntity<?> tryFindOrgInStore(InvocationOnMock invocation) {
        log.info("Trying to find the organization in store.");

        // Expecting a value like http://example.com/orgs/properties?scmBaseUrl=github.com&orgIdentity=myOrgId
        // or /orgs/properties?scmBaseUrl=github.com&orgIdentity=myOrgId
        // (depending on app properties)
        String scmId = queryStringInterceptor.getParamValue(invocation, "scmBaseUrl");
        String orgId = queryStringInterceptor.getParamValue(invocation, "orgIdentity");

        OrgPropertiesDto foundOrg = miniOrgStore.findOrg(orgId, scmId);

        ResponseEntity<?> fakeResponse;
        if (foundOrg != null) {
            log.info("The organization is found.");
            fakeResponse = ResponseEntity.ok(foundOrg);
        } else {
            log.info("The organization was not found, throwing a {} exception.", HttpStatus.NOT_FOUND);
            throw new HttpClientErrorException(HttpStatus.NOT_FOUND);
        }
        return fakeResponse;
    }
}
