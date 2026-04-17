package com.berdachuk.docurag.vector.internal;

import com.berdachuk.docurag.chunking.api.ChunkingApi;
import com.berdachuk.docurag.vector.api.EmbeddingIndexerApi;
import com.berdachuk.docurag.vector.api.IndexOperationsApi;
import com.berdachuk.docurag.vector.api.IndexStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IndexOperationsImpl implements IndexOperationsApi {

    private final ChunkingApi chunkingApi;
    private final EmbeddingIndexerApi embeddingIndexerApi;
    private final NamedParameterJdbcTemplate jdbc;
    private final IndexingProgressTracker progressTracker;

    @Override
    @Transactional
    public void rebuildFullIndex() {
        progressTracker.markChunkingPhase();
        chunkingApi.rebuildAllChunks();
        embeddingIndexerApi.embedAllMissing();
    }

    @Override
    @Transactional
    public int clearEmbeddings() {
        Integer updated = jdbc.update("UPDATE document_chunk SET embedding = NULL WHERE embedding IS NOT NULL", Map.of());
        return updated == null ? 0 : updated;
    }

    @Override
    @Transactional
    public int clearChunks() {
        Integer deleted = jdbc.update("DELETE FROM source_document", Map.of());
        return deleted == null ? 0 : deleted;
    }

    @Override
    public IndexStatus getStatus() {
        long docs = safeCount("SELECT COUNT(*) FROM source_document");
        long chunks = safeCount("SELECT COUNT(*) FROM document_chunk");
        long emb = safeCount("SELECT COUNT(*) FROM document_chunk WHERE embedding IS NOT NULL");

        List<IndexStatus> rows = jdbc.query("""
                        SELECT id, started_at, finished_at, status
                        FROM ingestion_job
                        ORDER BY started_at DESC
                        LIMIT 1
                        """, Map.of(), (rs, rowNum) -> new IndexStatus(
                docs,
                chunks,
                emb,
                rs.getString("id"),
                rs.getString("status"),
                rs.getObject("started_at", OffsetDateTime.class),
                rs.getObject("finished_at", OffsetDateTime.class)
        ));
        return rows.stream().findFirst().orElse(new IndexStatus(docs, chunks, emb, null, null, null, null));
    }

    private long safeCount(String sql) {
        Long n = jdbc.queryForObject(sql, Map.of(), Long.class);
        return n == null ? 0L : n;
    }
}
