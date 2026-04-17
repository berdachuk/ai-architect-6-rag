package com.berdachuk.docurag.web.pages;

import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AnalysisController {

    private final IndexOperationsApi indexOperationsApi;

    @GetMapping("/analysis")
    public String analysis(Model model) {
        return "analysis";
    }

    @PostMapping("/analysis/clear-old-data")
    public String clearOldAnalysisData(Model model) {
        int affected = indexOperationsApi.clearChunks();
        model.addAttribute(
                "analysisActionMessage",
                "Old data cleanup complete: deleted " + affected + " source document(s) and all associated chunks."
        );
        return "analysis";
    }
}