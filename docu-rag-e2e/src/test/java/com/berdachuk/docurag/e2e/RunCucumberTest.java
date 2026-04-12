package com.berdachuk.docurag.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "com.berdachuk.docurag.e2e.steps,com.berdachuk.docurag.e2e.ui")
@ConfigurationParameter(
        key = "cucumber.plugin",
        value = "pretty, summary, html:build/cucumber-reports/e2e-report.html, junit:build/cucumber-reports/cucumber.xml")
public class RunCucumberTest {
}
