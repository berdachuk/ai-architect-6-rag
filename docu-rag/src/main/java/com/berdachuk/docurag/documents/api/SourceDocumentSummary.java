package com.berdachuk.docurag.documents.api;

public record SourceDocumentSummary(
        String id,
        String externalId,
        String title,
        String category,
        String sourceFormat
) {
}
