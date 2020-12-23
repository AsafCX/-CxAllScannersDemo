package com.checkmarx.cxintegrations.reposmanager.api.cxflowconfiguration;

import java.util.ArrayList;
import java.util.List;

public class MiniStore {
    private final List<CxFlowConfig> orgs = new ArrayList<>();

    public void addCxFlowConfig(CxFlowConfig config) {
        orgs.add(config);
    }
}
