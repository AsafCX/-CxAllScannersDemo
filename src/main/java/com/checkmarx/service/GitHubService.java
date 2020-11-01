package com.checkmarx.service;

import com.checkmarx.dto.github.OrganizationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;


@Slf4j
@Service
public class GitHubService {
    private Map<String, String> synchronizedMap = Collections.synchronizedMap(new HashMap<String, String>());

    public void addAccessToken(String accesToken, List<OrganizationDto> orgsDto) {
        synchronized (synchronizedMap) {
            for (OrganizationDto orgDto : orgsDto) {
                if (synchronizedMap.containsKey(orgDto.getLogin())) {
                    synchronizedMap.remove(orgDto.getLogin());
                }
                synchronizedMap.put(orgDto.getLogin(), accesToken);
            }
        }
    }

    public String getAccessToken(String orgName) {
        String accessToken = "";
        synchronized (synchronizedMap) {
            if (synchronizedMap.containsKey(orgName)) {
                accessToken = synchronizedMap.get(orgName);
            }
        }
        return accessToken;
    }
}
