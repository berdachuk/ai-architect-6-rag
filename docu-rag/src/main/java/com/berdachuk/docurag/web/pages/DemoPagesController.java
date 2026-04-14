package com.berdachuk.docurag.web.pages;

import com.berdachuk.docurag.documents.api.DocumentCatalogApi;
import com.berdachuk.docurag.documents.api.DocumentIngestApi;
import com.berdachuk.docurag.documents.api.IngestSummary;
import com.berdachuk.docurag.evaluation.api.EvaluationApi;
import com.berdachuk.docurag.evaluation.api.EvaluationRunDetail;
import com.berdachuk.docurag.evaluation.api.EvaluationRunRequest;
import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import com.berdachuk.docurag.llm.api.RagAskResponse;
import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.vector.api.IndexStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class DemoPagesController {

    private final IndexOperationsApi indexOperationsApi;
    private final DocumentCatalogApi documentCatalogApi;
    private final DocumentIngestApi documentIngestApi;
    private final RagAskApi ragAskApi;
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

    @GetMapping("/qa")
    public String qaForm(Model model) {
        model.addAttribute("ragResult", null);
        return "qa";
    }

    @PostMapping("/qa")
    public String qaSubmit(
            Model model,
            @RequestParam String question,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.5") double minScore
    ) {
        RagAskResponse rag = ragAskApi.ask(new RagAskRequest(question, topK, minScore));
        model.addAttribute("ragResult", rag);
        model.addAttribute("question", question);
        model.addAttribute("topK", topK);
        model.addAttribute("minScore", minScore);
        return "qa";
    }

    @GetMapping("/documents")
    public String documents(Model model, @RequestParam(defaultValue = "0") int page) {
        model.addAttribute("documents", documentCatalogApi.listDocuments(page, 25));
        model.addAttribute("total", documentCatalogApi.countDocuments());
        model.addAttribute("page", page);
        return "documents";
    }

    @PostMapping("/documents/ingest")
    public String ingestConfigured(Model model) {
        IngestSummary s = documentIngestApi.ingestConfiguredPaths();
        model.addAttribute("ingestSummary", s);
        model.addAttribute("documents", documentCatalogApi.listDocuments(0, 25));
        model.addAttribute("total", documentCatalogApi.countDocuments());
        model.addAttribute("page", 0);
        return "documents";
    }

    @GetMapping("/analysis")
    public String analysis(Model model) {
        return "analysis";
    }

    @GetMapping("/evaluation")
    public String evaluationForm(Model model) {
        model.addAttribute("runs", evaluationApi.listRuns());
        model.addAttribute("runResult", null);
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
        var started = evaluationApi.run(new EvaluationRunRequest(datasetName, topK, minScore, semanticPassThreshold));
        model.addAttribute("runResult", started);
        model.addAttribute("runs", evaluationApi.listRuns());
        return "evaluation";
    }
}
