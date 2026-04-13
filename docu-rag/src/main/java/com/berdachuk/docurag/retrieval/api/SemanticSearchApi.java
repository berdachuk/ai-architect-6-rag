package com.berdachuk.docurag.retrieval.api;

import java.util.List;

public interface SemanticSearchApi {

    List<ChunkHit> search(String query, int topK, double minScore);
}
