package com.berdachuk.docurag.vector.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndexingProgressTrackerTest {

    @Test
    void snapshotIncludesProcessedIngestFiles() {
        IndexingProgressTracker tracker = new IndexingProgressTracker();
        tracker.start("run-1", "Started ingest.");

        tracker.markIngestFileProcessed(
                "/data/a.jsonl",
                "a.jsonl",
                3,
                1,
                750,
                1000,
                75,
                "LOADED",
                "Loaded 3, skipped 1."
        );

        var snapshot = tracker.snapshot();

        assertThat(snapshot.ingestFiles()).hasSize(1);
        assertThat(snapshot.ingestFiles().getFirst().name()).isEqualTo("a.jsonl");
        assertThat(snapshot.ingestFiles().getFirst().documentsLoaded()).isEqualTo(3);
        assertThat(snapshot.ingestFiles().getFirst().documentsSkipped()).isEqualTo(1);
        assertThat(snapshot.ingestFiles().getFirst().processedRecords()).isEqualTo(750);
        assertThat(snapshot.ingestFiles().getFirst().totalRecords()).isEqualTo(1000);
        assertThat(snapshot.ingestFiles().getFirst().processedPercent()).isEqualTo(75);
        assertThat(snapshot.message()).isEqualTo("Ingested files: 1.");
    }
}
