package com.berdachuk.docurag.llm.api;

public record RagAskRequest(
        String question,
        Integer topK,
        Double minScore
) {
}
