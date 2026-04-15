package com.berdachuk.docurag.e2e.steps;

import com.berdachuk.docurag.e2e.client.ApiException;
import com.berdachuk.docurag.e2e.client.model.AnalysisResponse;
import com.berdachuk.docurag.e2e.client.model.EvaluationRunDetail;
import com.berdachuk.docurag.e2e.client.model.EvaluationRunRequest;
import com.berdachuk.docurag.e2e.client.model.EvaluationRunStartedResponse;
import com.berdachuk.docurag.e2e.client.model.GraphVisualizationResponse;
import com.berdachuk.docurag.e2e.client.model.IngestPathsRequest;
import com.berdachuk.docurag.e2e.client.model.IngestSummary;
import com.berdachuk.docurag.e2e.client.model.IndexStatus;
import com.berdachuk.docurag.e2e.client.model.PieChartResponse;
import com.berdachuk.docurag.e2e.client.model.RagAskRequest;
import com.berdachuk.docurag.e2e.client.model.RagAskResponse;
import com.berdachuk.docurag.e2e.client.model.SourceDocumentDetail;
import com.berdachuk.docurag.e2e.client.model.SourceDocumentSummary;
import com.berdachuk.docurag.e2e.world.E2eState;
import com.berdachuk.docurag.e2e.world.E2eWorld;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiSteps {

    @SuppressWarnings("unused")
    private final TestContext testContext;

    private RagAskResponse lastRagAsk;
    private AnalysisResponse lastAnalysis;
    private String ragHistoryId;
    private Response lastApiResponse;

    public ApiSteps(TestContext testContext) {
        this.testContext = testContext;
    }

    @Given("actuator health is UP")
    public void actuatorHealthIsUp() throws Exception {
        Map<String, Object> h = E2eWorld.clients().actuator().getHealth();
        assertThat(topLevelOrComponentsUp(h)).as("health: %s", h).isTrue();
    }

    private static boolean topLevelOrComponentsUp(Map<String, Object> health) {
        if (health == null) {
            return false;
        }
        if ("UP".equals(health.get("status"))) {
            return true;
        }
        Object components = health.get("components");
        if (!(components instanceof Map<?, ?> map)) {
            return false;
        }
        boolean any = false;
        for (Object v : map.values()) {
            if (v instanceof Map<?, ?> part) {
                any = true;
                if (!"UP".equals(part.get("status"))) {
                    return false;
                }
            }
        }
        return any;
    }

    @When("I ingest the sample JSONL fixture")
    public void ingestSampleJsonl() throws Exception {
        IngestPathsRequest req = new IngestPathsRequest().paths(List.of(E2eWorld.fixtureJsonlPath()));
        IngestSummary s = E2eWorld.clients().documents().ingestDocuments(req);
        assertThat(s.getStatus()).isEqualTo("COMPLETED");
        int loaded = s.getDocumentsLoaded() == null ? 0 : s.getDocumentsLoaded();
        int skipped = s.getDocumentsSkipped() == null ? 0 : s.getDocumentsSkipped();
        assertThat(loaded + skipped).isGreaterThan(0);
    }

    @When("I ingest the tiny PDF fixture")
    public void ingestTinyPdf() throws Exception {
        IngestPathsRequest req = new IngestPathsRequest().paths(List.of(E2eWorld.fixturePdfPath()));
        IngestSummary s = E2eWorld.clients().documents().ingestDocuments(req);
        assertThat(s.getStatus()).isEqualTo("COMPLETED");
        int loaded = s.getDocumentsLoaded() == null ? 0 : s.getDocumentsLoaded();
        int skipped = s.getDocumentsSkipped() == null ? 0 : s.getDocumentsSkipped();
        assertThat(loaded + skipped).isGreaterThan(0);
    }

    @When("I list documents and remember the first id")
    public void listAndRememberFirstId() throws Exception {
        List<SourceDocumentSummary> docs = E2eWorld.clients().documents().listDocuments(0, 50);
        assertThat(docs).isNotEmpty();
        E2eState.firstDocumentId = docs.getFirst().getId();
        assertThat(E2eState.firstDocumentId).isNotBlank();
    }

    @Then("GET document by id returns detail")
    public void getDocumentByIdReturnsDetail() throws Exception {
        SourceDocumentDetail d = E2eWorld.clients().documents().getDocumentById(E2eState.firstDocumentId);
        assertThat(d.getId()).isEqualTo(E2eState.firstDocumentId);
    }

    @Then("document categories are available")
    public void documentCategoriesAvailable() throws Exception {
        List<String> cats = E2eWorld.clients().documents().listDocumentCategories();
        assertThat(cats).isNotNull();
    }

    @When("I trigger a full index rebuild")
    public void triggerRebuild() throws Exception {
        E2eWorld.clients().index().rebuildIndex();
    }

    @Then("embedded chunks become available within {int} minutes")
    public void waitEmbeddedChunks(int minutes) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(minutes));
        IndexStatus last = null;
        while (Instant.now().isBefore(deadline)) {
            last = E2eWorld.clients().index().getIndexStatus();
            Long emb = last.getEmbeddedChunkCount();
            if (emb != null && emb > 0) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Timeout waiting for embedded chunks, last status: " + last);
    }

    @Then("index status shows indexed content")
    public void indexStatusShowsContent() throws Exception {
        IndexStatus s = E2eWorld.clients().index().getIndexStatus();
        assertThat(s.getDocumentCount()).isNotNull().isGreaterThan(0);
        assertThat(s.getChunkCount()).isNotNull().isGreaterThan(0);
        assertThat(s.getEmbeddedChunkCount()).isNotNull().isGreaterThan(0);
    }

    @When("I POST incremental index expecting not implemented")
    public void postIncrementalNotImplemented() throws Exception {
        try {
            var resp = E2eWorld.clients().index().incrementalIndexWithHttpInfo();
            assertThat(resp.getStatusCode()).isEqualTo(501);
        } catch (ApiException e) {
            assertThat(e.getCode()).isEqualTo(501);
        }
    }

    @When("I ask RAG {string}")
    public void askRag(String question) throws Exception {
        RagAskRequest req = new RagAskRequest()
                .question(question)
                .topK(3)
                .minScore(0.0);
        lastRagAsk = E2eWorld.clients().rag().ragAsk(req);
    }

    @Then("the RAG response has an answer and retrieved chunks")
    public void ragResponseChunks() {
        assertThat(lastRagAsk).isNotNull();
        assertThat(lastRagAsk.getAnswer()).isNotBlank();
        assertThat(lastRagAsk.getRetrievedChunks()).isNotNull();
    }

    @Then("the RAG answer body contains {string}")
    public void ragAnswerContains(String fragment) {
        assertThat(lastRagAsk).isNotNull();
        assertThat(lastRagAsk.getAnswer()).contains(fragment);
    }

    @When("I run document analysis with defaults")
    public void runAnalysisDefaults() throws Exception {
        lastAnalysis = E2eWorld.clients().rag().ragAnalyze(null);
    }

    @Then("the analysis response is present")
    public void analysisPresent() {
        assertThat(lastAnalysis).isNotNull();
        assertThat(lastAnalysis.getCategories()).isNotNull();
    }

    @When("I fetch category pie visualization")
    public void fetchPie() throws Exception {
        PieChartResponse pie = E2eWorld.clients().visualizations().getCategoriesPie();
        assertThat(pie).isNotNull();
    }

    @Then("the pie response has chart data")
    public void pieHasData() throws Exception {
        PieChartResponse pie = E2eWorld.clients().visualizations().getCategoriesPie();
        assertThat(pie.getLabels()).isNotNull();
        assertThat(pie.getValues()).isNotNull();
    }

    @When("I fetch entity graph visualization")
    public void fetchGraph() throws Exception {
        GraphVisualizationResponse g = E2eWorld.clients().visualizations().getEntitiesGraph();
        assertThat(g).isNotNull();
    }

    @Then("the graph response has structure")
    public void graphHasStructure() throws Exception {
        GraphVisualizationResponse g = E2eWorld.clients().visualizations().getEntitiesGraph();
        assertThat(g.getNodes()).isNotNull();
        assertThat(g.getEdges()).isNotNull();
    }

    @When("I run evaluation for dataset {string}")
    public void runEvaluation(String dataset) throws Exception {
        EvaluationRunRequest req = new EvaluationRunRequest()
                .datasetName(dataset)
                .topK(3)
                .minScore(0.0)
                .semanticPassThreshold(0.5);
        EvaluationRunStartedResponse started = E2eWorld.clients().evaluation().runEvaluation(req);
        assertThat(started.getRunId()).isNotBlank();
        assertThat(started.getTotal()).isNotNull().isGreaterThan(0);
        E2eState.evaluationRunId = started.getRunId();
    }

    @Then("an evaluation run id is stored")
    public void evalIdStored() {
        assertThat(E2eState.evaluationRunId).isNotBlank();
    }

    @When("I list evaluation runs")
    public void listEvalRuns() throws Exception {
        var runs = E2eWorld.clients().evaluation().listEvaluationRuns();
        assertThat(runs).isNotEmpty();
    }

    @Then("the stored run appears in the list")
    public void storedRunInList() throws Exception {
        var runs = E2eWorld.clients().evaluation().listEvaluationRuns();
        boolean found = runs.stream().anyMatch(r -> E2eState.evaluationRunId.equals(r.getId()));
        assertThat(found).isTrue();
    }

    @When("I fetch evaluation run detail for the stored id")
    public void fetchEvalDetail() throws Exception {
        EvaluationRunDetail d = E2eWorld.clients().evaluation().getEvaluationRun(E2eState.evaluationRunId);
        assertThat(d.getSummary()).isNotNull();
        assertThat(d.getSummary().getId()).isEqualTo(E2eState.evaluationRunId);
    }

    @And("I fetch latest evaluation with a valid id")
    public void fetchLatestEval() throws Exception {
        EvaluationRunDetail latest = E2eWorld.clients().evaluation().getLatestEvaluation();
        assertThat(latest.getSummary()).isNotNull();
        assertThat(latest.getSummary().getId()).isNotBlank();
    }

    @When("I open the dashboard page")
    public void openDashboardPage() {
        // narrative step; assertion follows
    }

    @Then("the dashboard HTML contains {string}")
    public void dashboardContains(String fragment) {
        String body = given()
                .baseUri(E2eWorld.apiBaseUrl())
                .when()
                .get("/")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
        assertThat(body).contains(fragment);
    }

    @When("I request RAG history for id {string}")
    public void requestRagHistory(String id) {
        this.ragHistoryId = id;
    }

    @Then("the HTTP status is {int}")
    public void httpStatusIs(int expected) {
        assertThat(ragHistoryId).isNotBlank();
        given()
                .baseUri(E2eWorld.apiBaseUrl())
                .when()
                .get("/api/rag/history/" + ragHistoryId)
                .then()
                .statusCode(expected);
    }

    @When("I POST RAG ask without question")
    public void postRagAskWithoutQuestion() {
        lastApiResponse = given()
                .baseUri(E2eWorld.apiBaseUrl())
                .contentType("application/json")
                .body("{\"topK\":3,\"minScore\":0.0}")
                .when()
                .post("/api/rag/ask");
    }

    @When("I run evaluation without dataset name")
    public void runEvaluationWithoutDatasetName() {
        lastApiResponse = given()
                .baseUri(E2eWorld.apiBaseUrl())
                .contentType("application/json")
                .body("{\"topK\":3,\"minScore\":0.0,\"semanticPassThreshold\":0.5}")
                .when()
                .post("/api/evaluation/run");
    }

    @Then("the last API response status is {int}")
    public void lastApiResponseStatusIs(int expectedStatus) {
        assertThat(lastApiResponse).isNotNull();
        assertThat(lastApiResponse.statusCode()).isEqualTo(expectedStatus);
    }

    @Then("the last API response is problem detail")
    public void lastApiResponseIsProblemDetail() {
        assertThat(lastApiResponse).isNotNull();
        assertThat(lastApiResponse.contentType()).contains("application/problem+json");
        assertThat(lastApiResponse.jsonPath().getString("title")).isNotBlank();
        assertThat(lastApiResponse.jsonPath().getString("detail")).isNotBlank();
        assertThat(lastApiResponse.jsonPath().getInt("status")).isGreaterThanOrEqualTo(400);
    }
}
