package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.extraction.api.DocumentAnalysisApi;
import com.berdachuk.docurag.llm.api.RagAskApi;
import com.berdachuk.docurag.web.openapi.api.RagApi;
import com.berdachuk.docurag.web.openapi.model.AnalysisRequest;
import com.berdachuk.docurag.web.openapi.model.AnalysisResponse;
import com.berdachuk.docurag.web.openapi.model.CategoryCount;
import com.berdachuk.docurag.web.openapi.model.RagAskRequest;
import com.berdachuk.docurag.web.openapi.model.RagAskResponse;
import com.berdachuk.docurag.web.openapi.model.RetrievedChunkDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RagRestController implements RagApi {

    private final RagAskApi ragAskApi;
    private final DocumentAnalysisApi documentAnalysisApi;

    @Override
    public ResponseEntity<RagAskResponse> ragAsk(RagAskRequest request) {
        com.berdachuk.docurag.llm.api.RagAskRequest in = new com.berdachuk.docurag.llm.api.RagAskRequest(
                request.getQuestion(),
                request.getTopK(),
                request.getMinScore()
        );
        com.berdachuk.docurag.llm.api.RagAskResponse out = ragAskApi.ask(in);
        RagAskResponse payload = new RagAskResponse()
                .answer(out.answer())
                .model(out.model())
                .retrievedChunks(out.retrievedChunks().stream()
                        .map(c -> new RetrievedChunkDto()
                                .documentId(c.documentId())
                                .title(c.title())
                                .category(c.category())
                                .score(c.score())
                                .snippet(c.snippet()))
                        .toList());
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<AnalysisResponse> ragAnalyze(AnalysisRequest request) {
        com.berdachuk.docurag.extraction.api.AnalysisRequest in =
                new com.berdachuk.docurag.extraction.api.AnalysisRequest(request == null ? null : request.getMaxDocuments());
        com.berdachuk.docurag.extraction.api.AnalysisResponse out = documentAnalysisApi.analyze(in);
        AnalysisResponse payload = new AnalysisResponse()
                .categories(out.categories().stream()
                        .map(c -> new CategoryCount().category(c.category()).count(c.count()))
                        .toList())
                .graphNodes(out.graphNodes().stream()
                        .map(n -> new com.berdachuk.docurag.web.openapi.model.AnalysisGraphNode()
                                .id(n.id())
                                .label(n.label())
                                .type(n.type()))
                        .toList())
                .graphEdges(out.graphEdges().stream()
                        .map(e -> new com.berdachuk.docurag.web.openapi.model.AnalysisGraphEdge()
                                .source(e.source())
                                .target(e.target())
                                .relation(e.relation()))
                        .toList())
                .extractionNotes(out.extractionNotes());
        return ResponseEntity.ok(payload);
    }
}
