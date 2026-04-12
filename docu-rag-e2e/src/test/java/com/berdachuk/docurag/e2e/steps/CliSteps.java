package com.berdachuk.docurag.e2e.steps;

import com.berdachuk.docurag.e2e.infra.E2eInfraLifecycle;
import com.berdachuk.docurag.e2e.world.E2eWorld;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CliSteps {

    @SuppressWarnings("unused")
    private final TestContext testContext;

    private int lastCliExitCode = Integer.MIN_VALUE;

    public CliSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    @When("I run the eval-cli jar for dataset {string}")
    public void runEvalCli(String dataset) throws Exception {
        E2eInfraLifecycle.ensurePostgresForChildJvmCli();

        Path logFile = Path.of("target", "docurag-e2e-cli.log").toAbsolutePath();
        Files.createDirectories(logFile.getParent());

        List<String> cmd = new ArrayList<>();
        cmd.add(E2eWorld.javaBinary());
        cmd.add("-jar");
        cmd.add(E2eWorld.appJarPath().toString());
        cmd.add("--spring.profiles.active=e2e,eval-cli");
        cmd.add("--dataset=" + dataset);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("E2E_PG_HOST", "127.0.0.1");
        pb.environment().put("E2E_PG_PORT", String.valueOf(E2eWorld.pgPort()));
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

        Process p = pb.start();
        boolean finished = p.waitFor(15, TimeUnit.MINUTES);
        assertThat(finished).as("eval-cli timed out; see %s", logFile).isTrue();
        lastCliExitCode = p.exitValue();
    }

    @Then("the eval-cli process exits with code {int}")
    public void cliExitCode(int code) {
        assertThat(lastCliExitCode).isEqualTo(code);
    }

    @Then("the latest evaluation API returns a run id")
    public void latestEvalHasId() throws Exception {
        var latest = E2eWorld.clients().evaluation().getLatestEvaluation();
        assertThat(latest.getSummary()).isNotNull();
        assertThat(latest.getSummary().getId()).isNotBlank();
    }
}
