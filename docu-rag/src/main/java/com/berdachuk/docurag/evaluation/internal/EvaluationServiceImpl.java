package com.berdachuk.docurag.evaluation.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.berdachuk.docurag.core.config.CustomChatProperties;
import com.berdachuk.docurag.core.config.CustomEmbeddingProperties;
import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.core.util.IdGenerator;
import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationCaseResult;
import com.berdachuk.docurag.evaluation.api.EvaluationLogSnapshot;
import com.berdachuk.docurag.evaluation.api.EvaluationRunDetail;
import com.berdachuk.docurag.evaluation.api.EvaluationRunRequest;
import com.berdachuk.docurag.evaluation.api.EvaluationRunStartedResponse;
import com.berdachuk.docurag.evaluation.api.EvaluationRunSummary;
import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import com.berdachuk.docurag.llm.api.RagAskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationApi {

    private final EvaluationJdbcRepository repository;
    private final RagAskApi ragAskApi;
    private final EmbeddingModel embeddingModel;
    private final DocuRagProperties docuRagProperties;
    private final CustomChatProperties chatProperties;
    private final CustomEmbeddingProperties embeddingProperties;
    private final ObjectMapper objectMapper;
    private final EvaluationSampleDatasetSeeder datasetSeeder;
    private final EvaluationProgressTracker progressTracker;

    @Override
    public EvaluationRunStartedResponse run(EvaluationRunRequest request) {
        validateRequest(request);
        String datasetName = request.datasetName();
        datasetSeeder.ensureDatasetSeeded(request.datasetName());
        String datasetId = repository.findDatasetIdByName(request.datasetName())
                .orElseThrow(() -> new IllegalArgumentException("Unknown dataset: " + request.datasetName()));
        List<EvaluationJdbcRepository.EvalCaseRow> cases = repository.findCasesByDatasetId(datasetId);
        if (cases.isEmpty()) {
            throw new IllegalStateException("No evaluation cases for dataset");
        }
        int topK = request.topK() != null ? request.topK() : docuRagProperties.getRag().getDefaultTopK();
        double minScore = request.minScore() != null ? request.minScore() : docuRagProperties.getRag().getDefaultMinScore();
        double semTh = request.semanticPassThreshold() != null ? request.semanticPassThreshold() : 0.80;

        String runId = IdGenerator.generateId();
        int n = cases.size();
        progressTracker.start(runId, datasetName, n);
        progressTracker.log("Using topK=" + topK + ", minScore=" + minScore + ", semanticPassThreshold=" + semTh + ".");
        progressTracker.log("Chat model=" + chatProperties.getModel() + ", embedding model=" + embeddingProperties.getModel() + ".");
        OffsetDateTime started = OffsetDateTime.now();
        repository.insertRun(runId, datasetId, started, chatProperties.getModel(), embeddingProperties.getModel(), semTh);

        int normHits = 0;
        double semSum = 0;
        int semPassAt080 = 0;
        int semPass = 0;
        try {
            int caseNumber = 0;
            for (EvaluationJdbcRepository.EvalCaseRow c : cases) {
                caseNumber++;
                progressTracker.log("Case " + caseNumber + "/" + n + ": asking " + preview(c.question()));
                RagAskResponse rag = ragAskApi.ask(new RagAskRequest(c.question(), topK, minScore));
                String pred = rag.answer() == null ? "" : rag.answer().trim();
                String gt = c.groundTruth() == null ? "" : c.groundTruth().trim();
                boolean exact = pred.equalsIgnoreCase(gt);
                boolean norm = TextNormalization.normalize(pred).equals(TextNormalization.normalize(gt));
                if (norm) {
                    normHits++;
                }
                progressTracker.log("Case " + caseNumber + "/" + n + ": scoring answer.");
                float[] ev = embeddingModel.embed(new Document(pred));
                float[] gv = embeddingModel.embed(new Document(gt));
                double cos = VectorMath.cosineSimilarity(ev, gv);
                semSum += cos;
                if (cos >= 0.80) {
                    semPassAt080++;
                }
                boolean pass = cos >= semTh;
                if (pass) {
                    semPass++;
                }
                String chunksJson = toJson(rag);
                repository.insertResult(
                        IdGenerator.generateId(),
                        runId,
                        c.id(),
                        pred,
                        exact,
                        norm,
                        cos,
                        pass,
                        chunksJson
                );
                progressTracker.log("Case " + caseNumber + "/" + n + ": semantic similarity=" + round4(cos) + ", pass=" + pass + ".");
            }
            double normAcc = n == 0 ? 0 : (double) normHits / n;
            double meanSem = n == 0 ? 0 : semSum / n;
            double semAtThreshold = n == 0 ? 0 : (double) semPass / n;
            double sem080 = n == 0 ? 0 : (double) semPassAt080 / n;
            progressTracker.log("Writing evaluation summary.");
            repository.finishRun(runId, OffsetDateTime.now(), normAcc, meanSem, semAtThreshold, sem080);
            progressTracker.finish(runId);
            return new EvaluationRunStartedResponse(
                    runId,
                    n,
                    round4(normAcc),
                    round4(meanSem),
                    round4(semAtThreshold),
                    round4(semTh),
                    round4(sem080)
            );
        } catch (RuntimeException ex) {
            progressTracker.fail(runId, ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            throw ex;
        }
    }

    private static void validateRequest(EvaluationRunRequest request) {
        if (request.datasetName() == null || request.datasetName().isBlank()) {
            throw new IllegalArgumentException("datasetName is required");
        }
        if (request.topK() != null && request.topK() <= 0) {
            throw new IllegalArgumentException("topK must be > 0");
        }
        if (request.minScore() != null && (request.minScore() < 0.0 || request.minScore() > 1.0)) {
            throw new IllegalArgumentException("minScore must be between 0.0 and 1.0");
        }
        if (request.semanticPassThreshold() != null
            && (request.semanticPassThreshold() < 0.0 || request.semanticPassThreshold() > 1.0)) {
            throw new IllegalArgumentException("semanticPassThreshold must be between 0.0 and 1.0");
        }
    }

    private String toJson(RagAskResponse rag) {
        try {
            return objectMapper.writeValueAsString(rag.retrievedChunks());
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private static double round4(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public List<EvaluationRunSummary> listRuns() {
        return repository.listRuns().stream().map(this::toSummary).toList();
    }

    @Override
    public Optional<EvaluationRunDetail> getRun(String runId) {
        return repository.findRun(runId).map(row -> new EvaluationRunDetail(toSummary(row), mapResults(runId)));
    }

    @Override
    public Optional<EvaluationRunDetail> getLatestRun() {
        List<EvaluationJdbcRepository.RunRow> rows = repository.listRuns();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        EvaluationJdbcRepository.RunRow row = rows.getFirst();
        return Optional.of(new EvaluationRunDetail(toSummary(row), mapResults(row.id())));
    }

    @Override
    public int clearRuns() {
        int deleted = repository.deleteRuns();
        progressTracker.clear();
        progressTracker.log("Deleted " + deleted + " evaluation run(s) and associated result rows.");
        return deleted;
    }

    @Override
    public EvaluationLogSnapshot logs() {
        return progressTracker.snapshot();
    }

    private List<EvaluationCaseResult> mapResults(String runId) {
        List<EvaluationCaseResult> out = new ArrayList<>();
        for (EvaluationJdbcRepository.ResultRow r : repository.findResultsForRun(runId)) {
            out.add(new EvaluationCaseResult(
                    r.caseId(),
                    r.question(),
                    r.groundTruth(),
                    r.predicted(),
                    r.exactMatch(),
                    r.normalizedMatch(),
                    r.semanticSimilarity(),
                    r.semanticPass()
            ));
        }
        return out;
    }

    private EvaluationRunSummary toSummary(EvaluationJdbcRepository.RunRow row) {
        return new EvaluationRunSummary(
                row.id(),
                row.datasetId(),
                row.startedAt(),
                row.finishedAt(),
                row.modelName(),
                row.embeddingModelName(),
                row.normalizedAccuracy(),
                row.meanSemanticSimilarity(),
                row.semanticAccuracy(),
                row.semanticPassThreshold(),
                row.semanticAccuracyAt080()
        );
    }

    private static String preview(String text) {
        if (text == null || text.isBlank()) {
            return "(empty question)";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 117) + "...";
    }
}
