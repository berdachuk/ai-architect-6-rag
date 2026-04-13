package com.berdachuk.docurag.llm.api;

import java.util.List;

public record RagAskResponse(
        String answer,
        String model,
        List<RetrievedChunkDto> retrievedChunks
) {
}
