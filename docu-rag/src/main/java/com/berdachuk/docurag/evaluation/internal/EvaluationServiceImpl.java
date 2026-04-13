package com.berdachuk.docurag.evaluation.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.berdachuk.docurag.core.config.CustomChatProperties;
import com.berdachuk.docurag.core.config.CustomEmbeddingProperties;
import com.berdachuk.docurag.core.config.DocuRagProperties;
import com.berdachuk.docurag.core.util.IdGenerator;
import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationCaseResult;
import com.berdachuk.docurag.evaluation.api.EvaluationRunDetail;
import com.berdachuk.docurag.evaluation.api.EvaluationRunRequest;
import com.berdachuk.docurag.evaluation.api.EvaluationRunStartedResponse;
import com.berdachuk.docurag.evaluation.api.EvaluationRunSummary;
import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import com.berdachuk.docurag.llm.api.RagAskResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EvaluationServiceImpl implements EvaluationApi {

    private final EvaluationJdbcRepository repository;
    private final RagAskApi ragAskApi;
    private final EmbeddingModel embeddingModel;
    private final DocuRagProperties docuRagProperties;
    private final CustomChatProperties chatProperties;
    private final CustomEmbeddingProperties embeddingProperties;
    private final ObjectMapper objectMapper;

    public EvaluationServiceImpl(
            EvaluationJdbcRepository repository,
            RagAskApi ragAskApi,
            EmbeddingModel embeddingModel,
            DocuRagProperties docuRagProperties,
            CustomChatProperties chatProperties,
            CustomEmbeddingProperties embeddingProperties,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.ragAskApi = ragAskApi;
        this.embeddingModel = embeddingModel;
        this.docuRagProperties = docuRagProperties;
        this.chatProperties = chatProperties;
        this.embeddingProperties = embeddingProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public EvaluationRunStartedResponse run(EvaluationRunRequest request) {
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
        OffsetDateTime started = OffsetDateTime.now();
        repository.insertRun(runId, datasetId, started, chatProperties.getModel(), embeddingProperties.getModel());

        int normHits = 0;
        double semSum = 0;
        int semPass = 0;
        for (EvaluationJdbcRepository.EvalCaseRow c : cases) {
            RagAskResponse rag = ragAskApi.ask(new RagAskRequest(c.question(), topK, minScore));
            String pred = rag.answer() == null ? "" : rag.answer().trim();
            String gt = c.groundTruth() == null ? "" : c.groundTruth().trim();
            boolean exact = pred.equalsIgnoreCase(gt);
            boolean norm = TextNormalization.normalize(pred).equals(TextNormalization.normalize(gt));
            if (norm) {
                normHits++;
            }
            float[] ev = embeddingModel.embed(new Document(pred));
            float[] gv = embeddingModel.embed(new Document(gt));
            double cos = VectorMath.cosineSimilarity(ev, gv);
            semSum += cos;
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
        }
        int n = cases.size();
        double normAcc = n == 0 ? 0 : (double) normHits / n;
        double meanSem = n == 0 ? 0 : semSum / n;
        double sem080 = n == 0 ? 0 : (double) semPass / n;
        repository.finishRun(runId, OffsetDateTime.now(), normAcc, meanSem, sem080);
        return new EvaluationRunStartedResponse(runId, n, round4(normAcc), round4(meanSem), round4(sem080));
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
                row.semanticAccuracyAt080()
        );
    }
}
