package com.checkmarx.cxintegrations.reposmanager.api.scmconfiguration;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/scm-configuration-api.feature")
public class ScmConfigurationApiTest {
}
