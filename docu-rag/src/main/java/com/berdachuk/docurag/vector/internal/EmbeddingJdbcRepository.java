package com.berdachuk.docurag.vector.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EmbeddingJdbcRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public List<ChunkEmbeddingRow> findChunksWithoutEmbedding(int limit) {
        return jdbc.query("""
                        SELECT id, chunk_text FROM document_chunk
                        WHERE embedding IS NULL
                        ORDER BY document_id, chunk_index
                        LIMIT :limit
                        """, Map.of("limit", limit),
                (rs, rowNum) -> new ChunkEmbeddingRow(rs.getString("id"), rs.getString("chunk_text")));
    }

    public void updateEmbedding(String chunkId, String vectorLiteral) {
        jdbc.update(
                "UPDATE document_chunk SET embedding = CAST(:vectorLiteral AS vector) WHERE id = :chunkId",
                Map.of("chunkId", chunkId, "vectorLiteral", vectorLiteral)
        );
    }
}
