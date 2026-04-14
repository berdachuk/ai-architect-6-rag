package com.berdachuk.docurag.chunking.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ChunkJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public void deleteByDocumentId(String documentId) {
        jdbc.update(
                "DELETE FROM document_chunk WHERE document_id = :documentId",
                Map.of("documentId", documentId)
        );
    }

    public void deleteAll() {
        jdbc.update("DELETE FROM document_chunk", Map.of());
    }

    public void insertChunk(
            String id,
            String documentId,
            int chunkIndex,
            String chunkText,
            Integer tokenEstimate,
            String category
    ) {
        jdbc.update("""
                        INSERT INTO document_chunk (
                          id,
                          document_id,
                          chunk_index,
                          chunk_text,
                          token_estimate,
                          category,
                          embedding
                        ) VALUES (
                          :chunkId,
                          :documentId,
                          :chunkIndex,
                          :chunkText,
                          :tokenEstimate,
                          :category,
                          NULL
                        )
                        """, Map.of(
                "chunkId", id,
                "documentId", documentId,
                "chunkIndex", chunkIndex,
                "chunkText", chunkText,
                "tokenEstimate", tokenEstimate,
                "category", category
        ));
    }

    public long countChunks() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM document_chunk", Map.of(), Long.class);
        return n == null ? 0L : n;
    }

    public long countChunksWithEmbedding() {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_chunk WHERE embedding IS NOT NULL",
                Map.of(),
                Long.class
        );
        return n == null ? 0L : n;
    }
}
