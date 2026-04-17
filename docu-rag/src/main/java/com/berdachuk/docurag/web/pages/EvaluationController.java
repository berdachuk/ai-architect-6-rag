package com.berdachuk.docurag.web.pages;

import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationRunRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationApi evaluationApi;

    @GetMapping("/evaluation")
    public String evaluationForm(Model model) {
        model.addAttribute("runs", evaluationApi.listRuns());
        model.addAttribute("runResult", null);
        model.addAttribute("runError", null);
        model.addAttribute("highlightRunId", null);
        return "evaluation";
    }

    @PostMapping("/evaluation/run")
    public String evaluationRun(
            Model model,
            @RequestParam String datasetName,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.5") double minScore,
            @RequestParam(defaultValue = "0.8") double semanticPassThreshold
    ) {
        model.addAttribute("runError", null);
        try {
            var started = evaluationApi.run(new EvaluationRunRequest(datasetName, topK, minScore, semanticPassThreshold));
            model.addAttribute("runResult", started);
            model.addAttribute("highlightRunId", started.runId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("runResult", null);
            model.addAttribute("runError", ex.getMessage());
            model.addAttribute("highlightRunId", null);
        }
        model.addAttribute("runs", evaluationApi.listRuns());
        return "evaluation";
    }
}