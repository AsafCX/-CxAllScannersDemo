package com.checkmarx.cxintegrations.reposmanager.api.getrepos;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/cucumber/get-repos-api.feature",
        glue = {"com.checkmarx.cxintegrations.reposmanager.api.getrepos" } )
public class getReposApiRunner {
}
