package com.checkmarx.cxintegrations.reposmanager.api.cxflowconfiguration;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/cxflow-configuration-api.feature",
        extraGlue = "com.checkmarx.cxintegrations.reposmanager.api.shared")
public class CxFlowConfigApiTest {
}
