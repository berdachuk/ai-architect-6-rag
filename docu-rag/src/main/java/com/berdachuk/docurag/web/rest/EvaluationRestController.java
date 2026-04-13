package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationRunDetail;
import com.berdachuk.docurag.evaluation.api.EvaluationRunRequest;
import com.berdachuk.docurag.evaluation.api.EvaluationRunStartedResponse;
import com.berdachuk.docurag.evaluation.api.EvaluationRunSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationRestController {

    private final EvaluationApi evaluationApi;

    public EvaluationRestController(EvaluationApi evaluationApi) {
        this.evaluationApi = evaluationApi;
    }

    @PostMapping("/run")
    public EvaluationRunStartedResponse run(@RequestBody EvaluationRunRequest request) {
        return evaluationApi.run(request);
    }

    @GetMapping("/runs")
    public List<EvaluationRunSummary> runs() {
        return evaluationApi.listRuns();
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<EvaluationRunDetail> run(@PathVariable String id) {
        return evaluationApi.getRun(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    public ResponseEntity<EvaluationRunDetail> latest() {
        return evaluationApi.getLatestRun()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
