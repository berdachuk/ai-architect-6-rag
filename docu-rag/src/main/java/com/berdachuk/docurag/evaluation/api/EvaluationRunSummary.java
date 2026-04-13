package com.berdachuk.docurag.evaluation.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EvaluationRunSummary(
        String id,
        String datasetId,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String modelName,
        String embeddingModelName,
        BigDecimal normalizedAccuracy,
        BigDecimal meanSemanticSimilarity,
        BigDecimal semanticAccuracyAt080
) {
}
