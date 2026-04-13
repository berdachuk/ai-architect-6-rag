package com.berdachuk.docurag.chunking.internal;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DocumentContentReader {

    private final NamedParameterJdbcTemplate jdbc;

    public DocumentContentReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<DocumentRow> find(String documentId) {
        List<DocumentRow> rows = jdbc.query(
                "SELECT id, content, category FROM source_document WHERE id = :documentId",
                Map.of("documentId", documentId),
                (rs, rowNum) -> new DocumentRow(
                        rs.getString("id"),
                        rs.getString("content"),
                        rs.getString("category")
                )
        );
        return rows.stream().findFirst();
    }

    public record DocumentRow(String id, String content, String category) {
    }
}
