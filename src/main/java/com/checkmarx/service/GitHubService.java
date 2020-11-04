package com.checkmarx.service;

import com.checkmarx.dto.github.AccessTokenDto;
import com.checkmarx.dto.github.OrganizationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
public class GitHubService {
    private Map<String, AccessTokenDto> synchronizedMap =
            Collections.synchronizedMap(new HashMap<String, AccessTokenDto>());

    public void addAccessToken(AccessTokenDto accesToken, List<OrganizationDto> orgsDto) {
        synchronized (synchronizedMap) {
            for (OrganizationDto orgDto : orgsDto) {
                if (synchronizedMap.containsKey(orgDto.getLogin())) {
                    synchronizedMap.remove(orgDto.getLogin());
                }
                synchronizedMap.put(orgDto.getLogin(), accesToken);
            }
        }
    }

    public AccessTokenDto getAccessToken(String orgName) {
        AccessTokenDto accessToken = new AccessTokenDto();
        synchronized (synchronizedMap) {
            if (synchronizedMap.containsKey(orgName)) {
                accessToken = synchronizedMap.get(orgName);
            }
        }
        return accessToken;
    }
}
