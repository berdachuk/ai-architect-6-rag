package com.berdachuk.docurag.web.pages;

import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import com.berdachuk.docurag.llm.api.RagAskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class QaController {

    private final RagAskApi ragAskApi;

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
}