package com.berdachuk.docurag.documents.api;

public record IngestSummary(
        String jobId,
        int documentsLoaded,
        int documentsSkipped,
        String status,
        String errorMessage
) {
}
