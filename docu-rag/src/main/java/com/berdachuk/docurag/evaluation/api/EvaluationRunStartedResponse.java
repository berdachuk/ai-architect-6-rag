package com.berdachuk.docurag.evaluation.api;

public record EvaluationRunStartedResponse(
        String runId,
        int total,
        double normalizedAccuracy,
        double meanSemanticSimilarity,
        double semanticAccuracy,
        double semanticPassThreshold,
        double semanticAccuracyAt080
) {
}
