package com.berdachuk.docurag.web.pages;

import com.berdachuk.docurag.core.util.IdGenerator;
import com.berdachuk.docurag.documents.api.DocumentIngestApi;
import com.berdachuk.docurag.documents.api.IngestSummary;
import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.vector.api.IndexingProgressApi;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class DocumentIngestOrchestrator {

    private final DocumentIngestApi documentIngestApi;
    private final IndexOperationsApi indexOperationsApi;
    private final IndexingProgressApi progressTracker;
    private final TaskExecutor taskExecutor;

    public String startConfiguredIngestAndIndex() {
        return startAsync(() -> documentIngestApi.ingestConfiguredPaths(this::recordFileProgress), "Started ingest from configured paths.");
    }

    public String startPathIngestAndIndex(List<String> paths) {
        return startAsync(() -> documentIngestApi.ingestPaths(paths, this::recordFileProgress), "Started ingest from selected path.");
    }

    public String startUploadedPathIngestAndIndex(List<String> paths, Runnable cleanup) {
        return startAsync(() -> documentIngestApi.ingestPaths(paths, this::recordFileProgress), "Started ingest from uploaded files.", cleanup);
    }

    private String startAsync(Supplier<IngestSummary> ingestCall, String startMessage) {
        return startAsync(ingestCall, startMessage, () -> {
        });
    }

    private String startAsync(Supplier<IngestSummary> ingestCall, String startMessage, Runnable cleanup) {
        IndexingProgressApi.ProgressSnapshot current = progressTracker.snapshot();
        if (current.running()) {
            return current.runId();
        }
        String runId = IdGenerator.generateId();
        progressTracker.start(runId, startMessage);
        taskExecutor.execute(() -> runPipeline(ingestCall, cleanup));
        return runId;
    }

    private void runPipeline(Supplier<IngestSummary> ingestCall, Runnable cleanup) {
        try {
            IngestSummary summary = ingestCall.get();
            if (summary == null) {
                progressTracker.markFailed("Ingest failed: empty response.");
                return;
            }
            if (!"COMPLETED".equalsIgnoreCase(summary.status())) {
                String error = summary.errorMessage() == null ? "unknown error" : summary.errorMessage();
                progressTracker.markFailed("Ingest failed: " + error);
                return;
            }

            progressTracker.markIngestCompleted(
                    summary.jobId(),
                    summary.documentsLoaded(),
                    summary.documentsSkipped()
            );
            indexOperationsApi.rebuildFullIndex();
            progressTracker.markCompleted("Index rebuild finished (chunking + embeddings).");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            progressTracker.markFailed("Indexing failed: " + msg);
        } finally {
            try {
                cleanup.run();
            } catch (Exception ignored) {
                // Best effort cleanup.
            }
        }
    }

    private void recordFileProgress(com.berdachuk.docurag.documents.api.IngestProgressListener.IngestFileProgress event) {
        progressTracker.markIngestFileProcessed(
                event.path(),
                event.name(),
                event.documentsLoaded(),
                event.documentsSkipped(),
                event.status(),
                event.message()
        );
    }
}
