package com.berdachuk.docurag.retrieval.api;

public record ChunkHit(
        String chunkId,
        String documentId,
        String title,
        String category,
        double score,
        String snippet
) {
}
