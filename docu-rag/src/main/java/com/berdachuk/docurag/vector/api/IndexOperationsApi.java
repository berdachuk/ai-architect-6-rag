package com.berdachuk.docurag.vector.api;

public interface IndexOperationsApi {

    void rebuildFullIndex();

    int clearEmbeddings();

    int clearChunks();

    IndexStatus getStatus();
}
