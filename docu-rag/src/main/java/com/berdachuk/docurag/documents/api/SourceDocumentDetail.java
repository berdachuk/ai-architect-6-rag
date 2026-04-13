package com.berdachuk.docurag.documents.api;

public record SourceDocumentDetail(
        String id,
        String externalId,
        String title,
        String category,
        String sourceName,
        String sourceUrl,
        String content,
        String sourceFormat
) {
}
