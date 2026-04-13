package com.berdachuk.docurag.documents.internal;

record SourceDocumentEntity(
        String id,
        String externalId,
        String title,
        String category,
        String sourceName,
        String sourceUrl,
        String content,
        String contentHash,
        String sourceFormat
) {
}
