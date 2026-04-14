package com.berdachuk.docurag.e2e.steps;

import com.berdachuk.docurag.e2e.ui.PlaywrightContext;
import com.berdachuk.docurag.e2e.ui.PlaywrightHooks;
import com.berdachuk.docurag.e2e.infra.E2eInfraLifecycle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class UiSteps {

    private final PlaywrightContext playwright;

    public UiSteps(PlaywrightContext playwright) {
        this.playwright = playwright;
    }

    private Page currentPage() {
        Page p = playwright.getPage();
        assertThat(p).isNotNull();
        return p;
    }

    private String uiPath(String path) {
        String base = PlaywrightHooks.uiBase().replaceAll("/$", "");
        String p = path.startsWith("/") ? path : "/" + path;
        return base + p;
    }

    @Given("I open the UI path {string}")
    public void openPath(String path) {
        String url = uiPath(path);
        try {
            currentPage()
                    .navigate(
                            url,
                            new Page.NavigateOptions()
                                    .setWaitUntil(PlaywrightHooks.navigationWaitUntil())
                                    .setTimeout(90_000));
        } catch (PlaywrightException first) {
            if (!isConnectionRefused(first)) {
                throw first;
            }
            try {
                E2eInfraLifecycle.ensureStarted();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to recover E2E app availability", e);
            }
            currentPage()
                    .navigate(
                            url,
                            new Page.NavigateOptions()
                                    .setWaitUntil(PlaywrightHooks.navigationWaitUntil())
                                    .setTimeout(90_000));
        }
    }

    private static boolean isConnectionRefused(PlaywrightException e) {
        return e.getMessage() != null && e.getMessage().contains("ERR_CONNECTION_REFUSED");
    }

    @Then("the page shows the medical disclaimer fragment")
    public void disclaimerVisible() {
        assertThat(currentPage().content()).contains("Medical disclaimer:");
    }

    @Then("the title contains {string}")
    public void titleContains(String fragment) {
        assertThat(currentPage().title()).contains(fragment);
    }

    @When("I submit the question {string} on the QA page")
    public void submitQuestion(String question) {
        currentPage().locator("textarea[name='question']").fill(question);
        currentPage()
                .locator("button[type='submit']")
                .click(new Locator.ClickOptions().setNoWaitAfter(true));
    }

    @Then("the QA page shows an answer heading")
    public void answerHeading() {
        currentPage()
                .getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Answer").setExact(true))
                .waitFor();
    }

    @Then("the dashboard shows index section with document stats")
    public void dashboardIndexStats() {
        Page page = currentPage();
        page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Dashboard").setExact(true)).waitFor();
        assertThat(page.content()).contains("Index");
        assertThat(page.content()).contains("Documents:");
        assertThat(page.content()).contains("Chunks:");
        assertThat(page.content()).contains("Embedded chunks:");
    }

    @Then("the documents table has at least {int} row")
    public void documentsTableMinRows(int minRows) {
        int n = currentPage().locator("main table tbody tr").count();
        assertThat(n).isGreaterThanOrEqualTo(minRows);
    }

    @Then("the page body contains text {string}")
    public void pageBodyContains(String fragment) {
        assertThat(currentPage().content()).contains(fragment);
    }

    @When("I submit ingest configured paths on the documents page")
    public void submitIngestConfiguredPaths() {
        Page page = currentPage();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ingest configured paths"))
                .click(new Locator.ClickOptions().setNoWaitAfter(true));
        page.getByText(Pattern.compile("Job\\s+")).waitFor(new Locator.WaitForOptions().setTimeout(90_000));
    }

    @Then("the documents page shows ingest job completed successfully")
    public void documentsIngestCompleted() {
        String html = currentPage().content();
        assertThat(html).contains("COMPLETED");
        assertThat(html).doesNotContain("FAILED");
    }

    @When("I open the analysis page waiting for visualization APIs")
    public void openAnalysisWithVisualizationWaits() {
        Page page = currentPage();
        String url = uiPath("/analysis");
        page.waitForResponse(
                r -> r.url().contains("/api/visualizations/categories/pie") && r.request().method().equals("GET") && r.ok(),
                () ->
                        page.navigate(
                                url,
                                new Page.NavigateOptions()
                                        .setWaitUntil(PlaywrightHooks.navigationWaitUntil())
                                        .setTimeout(90_000)));
    }

    @Then("the pie chart area is visible")
    public void pieChartVisible() {
        currentPage().locator("#pieChart").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(90_000));
    }

    @Then("the entity graph shows a canvas")
    public void entityGraphCanvas() {
        currentPage()
                .locator("#entityGraph canvas")
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED).setTimeout(90_000));
    }

    @When("I run evaluation from the UI for dataset {string}")
    public void runEvaluationFromUi(String dataset) {
        Page page = currentPage();
        page.locator("input[name='datasetName']").fill(dataset);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Run evaluation"))
                .click(new Locator.ClickOptions().setNoWaitAfter(true));
        page.getByText("Normalized accuracy:").waitFor(new Locator.WaitForOptions().setTimeout(90_000));
    }

    @Then("the evaluation page shows run result with normalized accuracy")
    public void evaluationResultMetrics() {
        Page page = currentPage();
        assertThat(page.content()).contains("Normalized accuracy:");
        assertThat(page.content()).contains("Mean semantic similarity:");
    }

    @Then("the QA answer body is not empty")
    public void qaAnswerBodyNotEmpty() {
        Page page = currentPage();
        Locator answerCard =
                page.locator("main .card")
                        .filter(
                                new Locator.FilterOptions()
                                        .setHas(
                                                page.getByRole(
                                                        AriaRole.HEADING,
                                                        new Page.GetByRoleOptions().setName("Answer").setExact(true))));
        Locator paragraphs = answerCard.locator("p");
        int count = paragraphs.count();
        assertThat(count).isGreaterThan(0);
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < count; i++) {
            combined.append(paragraphs.nth(i).innerText().trim());
        }
        assertThat(combined.length()).isGreaterThan(20);
    }

    @When("I follow the nav link {string}")
    public void followNavLink(String linkName) {
        Page page = currentPage();
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(linkName))
                .click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    @Then("the browser path ends with {string}")
    public void browserPathEndsWith(String suffix) {
        String path = java.net.URI.create(currentPage().url()).getPath();
        assertThat(path).endsWith(suffix);
    }
}
