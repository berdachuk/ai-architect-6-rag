package com.berdachuk.docurag.web.pages;

import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationRunDetail;
import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.vector.api.IndexStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final IndexOperationsApi indexOperationsApi;
    private final EvaluationApi evaluationApi;

    @GetMapping("/")
    public String home(Model model) {
        IndexStatus status = indexOperationsApi.getStatus();
        model.addAttribute("indexStatus", status);
        model.addAttribute("docCount", status.documentCount());
        Optional<EvaluationRunDetail> latest = evaluationApi.getLatestRun();
        model.addAttribute("latestEval", latest.orElse(null));
        return "home";
    }
}