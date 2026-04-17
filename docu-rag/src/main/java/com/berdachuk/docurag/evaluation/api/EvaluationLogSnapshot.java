package com.berdachuk.docurag.evaluation.api;

import java.util.List;

public record EvaluationLogSnapshot(
        boolean running,
        String runId,
        boolean terminationRequested,
        List<String> entries
) {
}
