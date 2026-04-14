package com.berdachuk.docurag.documents.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DocumentJdbcRepository {

    private static final RowMapper<SourceDocumentEntity> SOURCE_DOCUMENT_ROW_MAPPER = (rs, rowNum) -> new SourceDocumentEntity(
            rs.getString("id"),
            rs.getString("external_id"),
            rs.getString("title"),
            rs.getString("category"),
            rs.getString("source_name"),
            rs.getString("source_url"),
            rs.getString("content"),
            rs.getString("content_hash"),
            rs.getString("source_format")
    );

    private final NamedParameterJdbcTemplate jdbc;

    public boolean existsByContentHash(String hash) {
        List<Integer> found = jdbc.query(
                "SELECT 1 FROM source_document WHERE content_hash = :contentHash LIMIT 1",
                Map.of("contentHash", hash),
                (rs, rowNum) -> 1
        );
        return !found.isEmpty();
    }

    public void insert(SourceDocumentEntity e) {
        jdbc.update("""
                        INSERT INTO source_document (
                          id,
                          external_id,
                          title,
                          category,
                          source_name,
                          source_url,
                          content,
                          content_hash,
                          source_format
                        ) VALUES (
                          :documentId,
                          :externalId,
                          :title,
                          :category,
                          :sourceName,
                          :sourceUrl,
                          :content,
                          :contentHash,
                          :sourceFormat
                        )
                        """, Map.of(
                "documentId", e.id(),
                "externalId", e.externalId(),
                "title", e.title(),
                "category", e.category(),
                "sourceName", e.sourceName(),
                "sourceUrl", e.sourceUrl(),
                "content", e.content(),
                "contentHash", e.contentHash(),
                "sourceFormat", e.sourceFormat()
        ));
    }

    public List<String> findAllIds() {
        return jdbc.queryForList("SELECT id FROM source_document ORDER BY created_at", Map.of(), String.class);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM source_document", Map.of(), Long.class);
        return n == null ? 0L : n;
    }

    public List<SourceDocumentEntity> findPage(int offset, int limit) {
        return jdbc.query("""
                        SELECT id, external_id, title, category, source_name, source_url, content, content_hash, source_format
                        FROM source_document
                        ORDER BY created_at
                        OFFSET :offset LIMIT :limit
                        """, Map.of("offset", offset, "limit", limit), SOURCE_DOCUMENT_ROW_MAPPER);
    }

    public Optional<SourceDocumentEntity> findById(String id) {
        List<SourceDocumentEntity> rows = jdbc.query("""
                        SELECT id, external_id, title, category, source_name, source_url, content, content_hash, source_format
                        FROM source_document
                        WHERE id = :documentId
                        """, Map.of("documentId", id), SOURCE_DOCUMENT_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<String> distinctCategories() {
        return jdbc.queryForList(
                "SELECT DISTINCT category FROM source_document WHERE category IS NOT NULL ORDER BY category",
                Map.of(),
                String.class
        );
    }
}
