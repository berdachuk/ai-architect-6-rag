package com.berdachuk.docurag.vector.api;

import java.time.OffsetDateTime;

public record IndexStatus(
        long documentCount,
        long chunkCount,
        long embeddedChunkCount,
        String lastIngestJobId,
        String lastIngestStatus,
        OffsetDateTime lastIngestStartedAt,
        OffsetDateTime lastIngestFinishedAt
) {
}
