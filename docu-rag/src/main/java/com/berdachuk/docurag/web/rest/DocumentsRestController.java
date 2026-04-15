package com.berdachuk.docurag.web.rest;

import com.berdachuk.docurag.documents.api.DocumentCatalogApi;
import com.berdachuk.docurag.documents.api.DocumentIngestApi;
import com.berdachuk.docurag.web.openapi.api.DocumentsApi;
import com.berdachuk.docurag.web.openapi.model.IngestPathsRequest;
import com.berdachuk.docurag.web.openapi.model.IngestSummary;
import com.berdachuk.docurag.web.openapi.model.SourceDocumentDetail;
import com.berdachuk.docurag.web.openapi.model.SourceDocumentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentsRestController implements DocumentsApi {

    private final DocumentIngestApi documentIngestApi;
    private final DocumentCatalogApi documentCatalogApi;

    @Override
    public ResponseEntity<IngestSummary> ingestDocuments(IngestPathsRequest body) {
        com.berdachuk.docurag.documents.api.IngestSummary summary = body != null && body.getPaths() != null && !body.getPaths().isEmpty()
                ? documentIngestApi.ingestPaths(body.getPaths())
                : documentIngestApi.ingestConfiguredPaths();
        return ResponseEntity.ok(toOpenApi(summary));
    }

    @Override
    public ResponseEntity<List<SourceDocumentSummary>> listDocuments(Integer page, Integer size) {
        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? 20 : size;
        List<SourceDocumentSummary> payload = documentCatalogApi.listDocuments(safePage, safeSize).stream()
                .map(this::toOpenApi)
                .toList();
        return ResponseEntity.ok(payload);
    }

    @Override
    public ResponseEntity<SourceDocumentDetail> getDocumentById(String id) {
        return documentCatalogApi.getDocument(id)
                .map(this::toOpenApi)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<String>> listDocumentCategories() {
        return ResponseEntity.ok(documentCatalogApi.listCategories());
    }

    private IngestSummary toOpenApi(com.berdachuk.docurag.documents.api.IngestSummary summary) {
        return new IngestSummary()
                .jobId(summary.jobId())
                .documentsLoaded(summary.documentsLoaded())
                .documentsSkipped(summary.documentsSkipped())
                .status(summary.status())
                .errorMessage(summary.errorMessage());
    }

    private SourceDocumentSummary toOpenApi(com.berdachuk.docurag.documents.api.SourceDocumentSummary summary) {
        return new SourceDocumentSummary()
                .id(summary.id())
                .externalId(summary.externalId())
                .title(summary.title())
                .category(summary.category())
                .sourceFormat(summary.sourceFormat());
    }

    private SourceDocumentDetail toOpenApi(com.berdachuk.docurag.documents.api.SourceDocumentDetail detail) {
        return new SourceDocumentDetail()
                .id(detail.id())
                .externalId(detail.externalId())
                .title(detail.title())
                .category(detail.category())
                .sourceName(detail.sourceName())
                .sourceUrl(detail.sourceUrl())
                .content(detail.content())
                .sourceFormat(detail.sourceFormat());
    }
}
