package com.berdachuk.docurag.vector.api;

public interface EmbeddingIndexerApi {

    /**
     * Embeds every chunk that currently has a null embedding. Returns number of rows updated.
     */
    int embedAllMissing();
}
