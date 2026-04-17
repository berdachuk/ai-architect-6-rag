package com.berdachuk.docurag.vector.api;

import java.time.OffsetDateTime;

public interface IndexingProgressApi {

    void start(String runId, String message);

    void stop();

    void markIngestCompleted(String ingestJobId, int loaded, int skipped);

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
            OffsetDateTime updatedAt
    ) {
    }
}