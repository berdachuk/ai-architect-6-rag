package com.berdachuk.docurag.retrieval.internal;

import com.berdachuk.docurag.core.util.PgVector;
import com.berdachuk.docurag.retrieval.api.ChunkHit;
import com.berdachuk.docurag.retrieval.api.SemanticSearchApi;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SemanticSearchImpl implements SemanticSearchApi {

    private static final int SNIPPET_MAX = 600;

    private final EmbeddingModel embeddingModel;
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public List<ChunkHit> search(String query, int topK, double minScore) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        int k = Math.min(50, Math.max(1, topK));
        float[] q = embeddingModel.embed(new Document(query));
        String vec = PgVector.toLiteral(q);
        List<ChunkHit> raw = jdbc.query("""
                        SELECT dc.id AS chunk_id, dc.document_id, dc.chunk_text,
                               sd.title, sd.category,
                               1 - (dc.embedding <=> CAST(:queryVector AS vector)) AS score
                        FROM document_chunk dc
                        JOIN source_document sd ON sd.id = dc.document_id
                        WHERE dc.embedding IS NOT NULL
                        ORDER BY dc.embedding <=> CAST(:queryVector AS vector)
                        LIMIT :topK
                        """, Map.of("queryVector", vec, "topK", k), (rs, rowNum) -> new ChunkHit(
                        rs.getString("chunk_id"),
                        rs.getString("document_id"),
                        rs.getString("title"),
                        rs.getString("category"),
                        rs.getDouble("score"),
                        truncate(rs.getString("chunk_text"), SNIPPET_MAX)
                ));
        List<ChunkHit> filtered = new ArrayList<>();
        for (ChunkHit h : raw) {
            if (h.score() >= minScore) {
                filtered.add(h);
            }
        }
        return filtered;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }
}
