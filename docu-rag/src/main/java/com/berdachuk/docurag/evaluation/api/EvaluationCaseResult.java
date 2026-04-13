package com.berdachuk.docurag.evaluation.api;

import java.math.BigDecimal;

public record EvaluationCaseResult(
        String caseId,
        String question,
        String groundTruth,
        String predictedAnswer,
        boolean exactMatch,
        boolean normalizedMatch,
        BigDecimal semanticSimilarity,
        boolean semanticPass
) {
}
