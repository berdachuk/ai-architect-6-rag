package com.berdachuk.docurag.extraction.api;

public record GraphEdge(
        String source,
        String target,
        String relation
) {
}
