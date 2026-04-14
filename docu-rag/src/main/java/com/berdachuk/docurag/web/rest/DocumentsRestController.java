package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.documents.api.DocumentCatalogApi;
import com.berdachuk.docurag.documents.api.DocumentIngestApi;
import com.berdachuk.docurag.documents.api.IngestSummary;
import com.berdachuk.docurag.documents.api.SourceDocumentDetail;
import com.berdachuk.docurag.documents.api.SourceDocumentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentsRestController {

    private final DocumentIngestApi documentIngestApi;
    private final DocumentCatalogApi documentCatalogApi;

    @PostMapping("/ingest")
    public ResponseEntity<IngestSummary> ingest(@RequestBody(required = false) IngestPathsRequest body) {
        IngestSummary summary = body != null && body.paths() != null && !body.paths().isEmpty()
                ? documentIngestApi.ingestPaths(body.paths())
                : documentIngestApi.ingestConfiguredPaths();
        return ResponseEntity.ok(summary);
    }

    @GetMapping
    public List<SourceDocumentSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return documentCatalogApi.listDocuments(page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SourceDocumentDetail> get(@PathVariable String id) {
        return documentCatalogApi.getDocument(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return documentCatalogApi.listCategories();
    }
}
