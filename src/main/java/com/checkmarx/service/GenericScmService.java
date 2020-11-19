package com.checkmarx.service;

import com.checkmarx.controller.DataController;
import com.checkmarx.controller.exception.ScmException;
import com.checkmarx.dto.AccessTokenDto;
import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.dto.datastore.RepoDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.web.OrgSettingsWebDto;

import com.checkmarx.dto.web.RepoWebDto;
import com.checkmarx.dto.web.ScmConfigWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;

import java.util.Map;

@Slf4j
@Service("AbstractScmService")
public class GenericScmService {
    
    @Autowired
    DataController dataStoreController;

    @Autowired
    RestHelper restHelper;


    public RepoWebDto getScmOrgRepo(@NonNull String orgName, @NonNull String repoName, String baseUrl) {
        RepoDto repoDto = dataStoreController.getScmOrgRepo(baseUrl, orgName, repoName);
        return Converter.convertRepoDtoToRepoWebDto(repoDto);
    }
    
    private Map<String, AccessTokenDto> synchronizedMap =
            Collections.synchronizedMap(new HashMap<>());


    public ScmConfigWebDto getScmConfiguration(String baseUrl, String scopes) {
        ScmDto scmDto = dataStoreController.getScm(baseUrl);
        return ScmConfigWebDto.builder().clientId(scmDto.getClientId()).scope(scopes).build();
    }

     
    public OrgSettingsWebDto getOrgSettings(@NonNull String orgName, String baseUrl) {
        OrgPropertiesDto orgPropertiesDto = dataStoreController.getScmOrgSettings(baseUrl,
                orgName);
        return Converter.convertOrgProToOrgSettingsWebDto(orgPropertiesDto);
    }


    public void setOrgSettings(@NonNull String orgName, @NonNull OrgSettingsWebDto orgSettingsWebDto, String baseUrl) {
        OrgPropertiesDto orgPropertiesDto = Converter.convertToCxFlowProperties(baseUrl,
                orgName,
                orgSettingsWebDto);
        dataStoreController.storeScmOrgSettings(orgPropertiesDto);
    }
}
