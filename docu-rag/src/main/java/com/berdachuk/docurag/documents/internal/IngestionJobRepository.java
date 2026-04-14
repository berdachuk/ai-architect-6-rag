package com.berdachuk.docurag.documents.internal;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IngestionJobRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public void insertStarted(String id, OffsetDateTime started) {
        jdbc.update("""
                        INSERT INTO ingestion_job (
                          id,
                          started_at,
                          status,
                          documents_loaded,
                          documents_indexed
                        ) VALUES (
                          :jobId,
                          :startedAt,
                          'RUNNING',
                          0,
                          0
                        )
                        """, Map.of(
                "jobId", id,
                "startedAt", started
        ));
    }

    public void finishSuccess(String id, int loaded, OffsetDateTime finished) {
        jdbc.update("""
                        UPDATE ingestion_job
                        SET finished_at = :finishedAt,
                            status = 'COMPLETED',
                            documents_loaded = :documentsLoaded,
                            error_message = NULL
                        WHERE id = :jobId
                        """, Map.of(
                "jobId", id,
                "finishedAt", finished,
                "documentsLoaded", loaded
        ));
    }

    public void finishFailure(String id, String message, OffsetDateTime finished) {
        jdbc.update("""
                        UPDATE ingestion_job
                        SET finished_at = :finishedAt,
                            status = 'FAILED',
                            error_message = :errorMessage
                        WHERE id = :jobId
                        """, Map.of(
                "jobId", id,
                "finishedAt", finished,
                "errorMessage", message
        ));
    }

    public Optional<IngestionJobRow> findLatest() {
        List<IngestionJobRow> rows = jdbc.query("""
                        SELECT id, started_at, finished_at, status, documents_loaded, documents_indexed, error_message
                        FROM ingestion_job
                        ORDER BY started_at DESC
                        LIMIT 1
                        """, Map.of(), (rs, rowNum) -> new IngestionJobRow(
                        rs.getString("id"),
                        rs.getObject("started_at", OffsetDateTime.class),
                        rs.getObject("finished_at", OffsetDateTime.class),
                        rs.getString("status"),
                        rs.getInt("documents_loaded"),
                        rs.getInt("documents_indexed"),
                        rs.getString("error_message")
                ));
        return rows.stream().findFirst();
    }

    public record IngestionJobRow(
            String id,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            String status,
            int documentsLoaded,
            int documentsIndexed,
            String errorMessage
    ) {
    }
}
