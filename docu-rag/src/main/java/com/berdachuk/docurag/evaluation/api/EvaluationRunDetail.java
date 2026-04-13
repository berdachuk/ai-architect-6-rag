package com.berdachuk.docurag.evaluation.api;

import java.util.List;

public record EvaluationRunDetail(
        EvaluationRunSummary summary,
        List<EvaluationCaseResult> cases
) {
}
