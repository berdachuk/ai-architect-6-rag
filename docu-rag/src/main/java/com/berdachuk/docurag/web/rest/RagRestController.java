package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.extraction.api.AnalysisRequest;
import com.berdachuk.docurag.extraction.api.AnalysisResponse;
import com.berdachuk.docurag.extraction.api.DocumentAnalysisApi;
import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.llm.api.RagAskRequest;
import com.berdachuk.docurag.llm.api.RagAskResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagRestController {

    private final RagAskApi ragAskApi;
    private final DocumentAnalysisApi documentAnalysisApi;

    public RagRestController(RagAskApi ragAskApi, DocumentAnalysisApi documentAnalysisApi) {
        this.ragAskApi = ragAskApi;
        this.documentAnalysisApi = documentAnalysisApi;
    }

    @PostMapping("/ask")
    public RagAskResponse ask(@RequestBody RagAskRequest request) {
        return ragAskApi.ask(request);
    }

    @PostMapping("/analyze")
    public AnalysisResponse analyze(@RequestBody(required = false) AnalysisRequest request) {
        AnalysisRequest req = request == null ? new AnalysisRequest(null) : request;
        return documentAnalysisApi.analyze(req);
    }
}
