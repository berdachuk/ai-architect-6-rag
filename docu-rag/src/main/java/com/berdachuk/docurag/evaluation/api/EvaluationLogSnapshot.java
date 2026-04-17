package com.berdachuk.docurag.evaluation.api;

import java.util.List;

public record EvaluationLogSnapshot(
        boolean running,
        String runId,
        List<String> entries
) {
}
