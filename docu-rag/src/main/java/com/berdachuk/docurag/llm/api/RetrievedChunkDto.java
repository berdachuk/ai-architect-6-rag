package com.berdachuk.docurag.llm.api;

public record RetrievedChunkDto(
        String documentId,
        String title,
        String category,
        double score,
        String snippet
) {
}
