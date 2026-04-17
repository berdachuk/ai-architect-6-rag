package com.berdachuk.docurag.vector.api;

import java.time.OffsetDateTime;
import java.util.List;

public interface IndexingProgressApi {

    void start(String runId, String message);

    void stop();

    void markIngestCompleted(String ingestJobId, int loaded, int skipped);

    void markIngestFileProcessed(
            String path,
            String name,
            int documentsLoaded,
            int documentsSkipped,
            String status,
            String message
    );

    void markChunkingPhase();

    void markEmbeddingPhase(int totalChunksToEmbed);

    void markEmbeddingProgress(int embeddedChunks);

    void markCompleted(String message);

    void markFailed(String error);

    ProgressSnapshot snapshot();

    record ProgressSnapshot(
            boolean running,
            String runId,
            String phase,
            int embeddedChunks,
            int totalChunksToEmbed,
            String message,
            String ingestJobId,
            OffsetDateTime startedAt,
            OffsetDateTime updatedAt,
            List<IngestFileProgress> ingestFiles
    ) {
    }

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
