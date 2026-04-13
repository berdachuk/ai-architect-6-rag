package com.berdachuk.docurag.evaluation.api;

import java.util.List;
import java.util.Optional;

public interface EvaluationApi {

    EvaluationRunStartedResponse run(EvaluationRunRequest request);

    List<EvaluationRunSummary> listRuns();

    Optional<EvaluationRunDetail> getRun(String runId);

    Optional<EvaluationRunDetail> getLatestRun();
}
