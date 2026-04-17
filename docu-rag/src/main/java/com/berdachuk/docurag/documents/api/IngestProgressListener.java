package com.berdachuk.docurag.documents.api;

import java.time.OffsetDateTime;

@FunctionalInterface
public interface IngestProgressListener {

    IngestProgressListener NOOP = event -> {
    };

    void onFileProcessed(IngestFileProgress event);

    record IngestFileProgress(
            String path,
            String name,
            int documentsLoaded,
            int documentsSkipped,
            String status,
            String message,
            OffsetDateTime updatedAt
    ) {
    }
}
