package com.berdachuk.docurag.vector.internal;

import com.berdachuk.docurag.vector.api.IndexingProgressApi;
import com.berdachuk.docurag.vector.api.IndexingProgressApi.ProgressSnapshot;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class IndexingProgressTracker implements IndexingProgressApi {

    private boolean running;
    private String runId;
    private String phase;
    private int embeddedChunks;
    private int totalChunksToEmbed;
    private String message;
    private String ingestJobId;
    private OffsetDateTime startedAt;
    private OffsetDateTime updatedAt;

    public synchronized void start(String runId, String message) {
        this.running = true;
        this.runId = runId;
        this.phase = "INGESTING";
        this.embeddedChunks = 0;
        this.totalChunksToEmbed = 0;
        this.message = message;
        this.ingestJobId = null;
        this.startedAt = OffsetDateTime.now();
        this.updatedAt = this.startedAt;
    }

    public synchronized void markIngestCompleted(String ingestJobId, int loaded, int skipped) {
        if (!running) {
            return;
        }
        this.ingestJobId = ingestJobId;
        this.message = "Ingest completed: loaded " + loaded + ", skipped " + skipped + ".";
        this.updatedAt = OffsetDateTime.now();
    }

    public synchronized void markChunkingPhase() {
        if (!running) {
            return;
        }
        this.phase = "CHUNKING";
        this.message = "Chunk generation in progress.";
        this.updatedAt = OffsetDateTime.now();
    }

    public synchronized void markEmbeddingPhase(int totalChunksToEmbed) {
        if (!running) {
            return;
        }
        this.phase = "EMBEDDING";
        this.totalChunksToEmbed = Math.max(0, totalChunksToEmbed);
        this.embeddedChunks = 0;
        this.message = "Embedding generation in progress.";
        this.updatedAt = OffsetDateTime.now();
    }

    public synchronized void markEmbeddingProgress(int embeddedChunks) {
        if (!running) {
            return;
        }
        this.embeddedChunks = Math.max(0, embeddedChunks);
        this.updatedAt = OffsetDateTime.now();
    }

    public synchronized void markCompleted(String message) {
        if (!running) {
            return;
        }
        this.phase = "COMPLETED";
        this.running = false;
        this.message = message;
        this.updatedAt = OffsetDateTime.now();
    }

    public synchronized void markFailed(String error) {
        this.phase = "FAILED";
        this.running = false;
        this.message = error;
        this.updatedAt = OffsetDateTime.now();
    }

    @Override
    public synchronized ProgressSnapshot snapshot() {
        return new ProgressSnapshot(
                running,
                runId,
                phase,
                embeddedChunks,
                totalChunksToEmbed,
                message,
                ingestJobId,
                startedAt,
                updatedAt
        );
    }
}
