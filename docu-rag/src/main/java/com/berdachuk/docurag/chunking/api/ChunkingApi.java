package com.berdachuk.docurag.chunking.api;

public interface ChunkingApi {

    /**
     * Deletes all chunks and rebuilds from every source document.
     */
    int rebuildAllChunks();

    void rebuildChunksForDocument(String documentId);
}
