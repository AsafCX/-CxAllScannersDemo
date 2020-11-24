package com.checkmarx.service;

import com.checkmarx.dto.cxflow.CxFlowConfigDto;
import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import com.checkmarx.dto.datastore.ScmDto;
import com.checkmarx.dto.web.OrgSettingsWebDto;
import com.checkmarx.dto.web.ScmConfigWebDto;
import com.checkmarx.utils.Converter;
import com.checkmarx.utils.RestHelper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("ConfigurationService")
public class ConfigurationService {
    
    @Autowired
    DataService dataStoreService;

    @Autowired
    RestHelper restHelper;


    public ScmConfigWebDto getScmConfiguration(String baseUrl, String scopes) {
        ScmDto scmDto = dataStoreService.getScm(baseUrl);
        return ScmConfigWebDto.builder().clientId(scmDto.getClientId()).scope(scopes).build();
    }

     
    public OrgSettingsWebDto getOrgSettings(@NonNull String orgName, String baseUrl) {
        OrgPropertiesDto orgPropertiesDto = dataStoreService.getScmOrgSettings(baseUrl,
                                                                               orgName);
        return Converter.convertOrgProToOrgSettingsWebDto(orgPropertiesDto);
    }


    public void setOrgSettings(@NonNull String orgName, @NonNull OrgSettingsWebDto orgSettingsWebDto, String baseUrl) {
        OrgPropertiesDto orgPropertiesDto = Converter.convertToCxFlowProperties(baseUrl,
                orgName,
                orgSettingsWebDto);
        dataStoreService.storeScmOrgSettings(orgPropertiesDto);
    }

    public CxFlowConfigDto getCxFlowConfiguration(@NonNull String orgName, String baseUrl) {
        ScmAccessTokenDto scmAccessTokenDto = dataStoreService.getSCMOrgToken(baseUrl, orgName);
        OrgPropertiesDto orgPropertiesDto = dataStoreService.getScmOrgSettings(baseUrl, orgName);
        return CxFlowConfigDto.builder()
                .team(orgPropertiesDto.getCxTeam())
                .cxgoSecret(orgPropertiesDto.getCxGoToken())
                .token(scmAccessTokenDto.getAccessToken())
                .build();
    }

}
