package com.berdachuk.docurag.evaluation.api;

public record EvaluationRunRequest(
        String datasetName,
        Integer topK,
        Double minScore,
        Double semanticPassThreshold
) {
}
