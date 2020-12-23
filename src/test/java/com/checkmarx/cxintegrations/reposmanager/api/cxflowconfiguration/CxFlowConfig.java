package com.checkmarx.cxintegrations.reposmanager.api.cxflowconfiguration;

import com.checkmarx.dto.datastore.OrgPropertiesDto;
import com.checkmarx.dto.datastore.ScmAccessTokenDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class CxFlowConfig {
    private OrgPropertiesDto orgProperties;
    private ScmAccessTokenDto scmAccessToken;
}
