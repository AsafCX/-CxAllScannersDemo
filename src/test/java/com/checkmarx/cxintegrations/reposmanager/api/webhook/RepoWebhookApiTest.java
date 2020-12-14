package com.checkmarx.cxintegrations.reposmanager.api.webhook;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/repo-webhook-api.feature",
        extraGlue = "com.checkmarx.cxintegrations.reposmanager.api.shared")
public class RepoWebhookApiTest {
}
