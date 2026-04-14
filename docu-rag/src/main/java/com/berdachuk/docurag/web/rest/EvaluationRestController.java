package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.web.openapi.model.EvaluationCaseResult;
import com.berdachuk.docurag.web.openapi.model.EvaluationRunDetail;
import com.berdachuk.docurag.web.openapi.model.EvaluationRunRequest;
import com.berdachuk.docurag.web.openapi.model.EvaluationRunStartedResponse;
import com.berdachuk.docurag.web.openapi.model.EvaluationRunSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EvaluationRestController implements com.berdachuk.docurag.web.openapi.api.EvaluationApi {

    private final EvaluationApi evaluationApi;

    @Override
    public ResponseEntity<EvaluationRunStartedResponse> runEvaluation(EvaluationRunRequest request) {
        com.berdachuk.docurag.evaluation.api.EvaluationRunRequest in = new com.berdachuk.docurag.evaluation.api.EvaluationRunRequest(
                request.getDatasetName(),
                request.getTopK(),
                request.getMinScore(),
                request.getSemanticPassThreshold()
        );
        com.berdachuk.docurag.evaluation.api.EvaluationRunStartedResponse out = evaluationApi.run(in);
        EvaluationRunStartedResponse payload = new EvaluationRunStartedResponse()
                .runId(out.runId())
                .total(out.total())
                .normalizedAccuracy(out.normalizedAccuracy())
                .meanSemanticSimilarity(out.meanSemanticSimilarity())
                .semanticAccuracyAt080(out.semanticAccuracyAt080());
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<List<EvaluationRunSummary>> listEvaluationRuns() {
        List<EvaluationRunSummary> payload = evaluationApi.listRuns().stream()
                .map(this::toOpenApiSummary)
                .toList();
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<EvaluationRunDetail> getEvaluationRun(String id) {
        return evaluationApi.getRun(id)
                .map(this::toOpenApiDetail)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<EvaluationRunDetail> getLatestEvaluation() {
        return evaluationApi.getLatestRun()
                .map(this::toOpenApiDetail)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private EvaluationRunSummary toOpenApiSummary(com.berdachuk.docurag.evaluation.api.EvaluationRunSummary summary) {
        return new EvaluationRunSummary()
                .id(summary.id())
                .datasetId(summary.datasetId())
                .startedAt(summary.startedAt())
                .finishedAt(summary.finishedAt())
                .modelName(summary.modelName())
                .embeddingModelName(summary.embeddingModelName())
                .normalizedAccuracy(summary.normalizedAccuracy())
                .meanSemanticSimilarity(summary.meanSemanticSimilarity())
                .semanticAccuracyAt080(summary.semanticAccuracyAt080());
    }

    private EvaluationRunDetail toOpenApiDetail(com.berdachuk.docurag.evaluation.api.EvaluationRunDetail detail) {
        List<EvaluationCaseResult> cases = detail.cases().stream()
                .map(c -> new EvaluationCaseResult()
                        .caseId(c.caseId())
                        .question(c.question())
                        .groundTruth(c.groundTruth())
                        .predictedAnswer(c.predictedAnswer())
                        .exactMatch(c.exactMatch())
                        .normalizedMatch(c.normalizedMatch())
                        .semanticSimilarity(c.semanticSimilarity())
                        .semanticPass(c.semanticPass()))
                .toList();
        return new EvaluationRunDetail()
                .summary(toOpenApiSummary(detail.summary()))
                .cases(cases);
    }
}
