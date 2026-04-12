package com.berdachuk.docurag.vector.api;

public interface IndexOperationsApi {

    void rebuildFullIndex();

    IndexStatus getStatus();
}
