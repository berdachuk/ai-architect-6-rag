package com.berdachuk.docurag.evaluation.internal;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EvaluationJdbcRepository {

    private static final RowMapper<RunRow> RUN_ROW_MAPPER = (rs, rowNum) -> new RunRow(
            rs.getString("id"),
            rs.getString("dataset_id"),
            rs.getObject("started_at", OffsetDateTime.class),
            rs.getObject("finished_at", OffsetDateTime.class),
            rs.getString("model_name"),
            rs.getString("embedding_model_name"),
            rs.getBigDecimal("normalized_accuracy"),
            rs.getBigDecimal("mean_semantic_similarity"),
            rs.getBigDecimal("semantic_accuracy_at_080")
    );

    private final NamedParameterJdbcTemplate jdbc;

    public Optional<String> findDatasetIdByName(String name) {
        List<String> ids = jdbc.queryForList(
                "SELECT id FROM evaluation_dataset WHERE name = :datasetName",
                Map.of("datasetName", name),
                String.class
        );
        return ids.stream().findFirst();
    }

    public List<EvalCaseRow> findCasesByDatasetId(String datasetId) {
        return jdbc.query("""
                        SELECT id, question, ground_truth_answer
                        FROM evaluation_case
                        WHERE dataset_id = :datasetId
                        ORDER BY external_case_id
                        """, Map.of("datasetId", datasetId), (rs, rowNum) -> new EvalCaseRow(
                        rs.getString("id"),
                        rs.getString("question"),
                        rs.getString("ground_truth_answer")
                ));
    }

    public void insertRun(
            String id,
            String datasetId,
            OffsetDateTime started,
            String modelName,
            String embeddingModelName
    ) {
        jdbc.update("""
                        INSERT INTO evaluation_run (
                          id,
                          dataset_id,
                          started_at,
                          model_name,
                          embedding_model_name
                        ) VALUES (
                          :runId,
                          :datasetId,
                          :startedAt,
                          :modelName,
                          :embeddingModelName
                        )
                        """, Map.of(
                "runId", id,
                "datasetId", datasetId,
                "startedAt", started,
                "modelName", modelName,
                "embeddingModelName", embeddingModelName
        ));
    }

    public void finishRun(
            String id,
            OffsetDateTime finished,
            double normalizedAccuracy,
            double meanSemantic,
            double semanticAt080
    ) {
        jdbc.update("""
                        UPDATE evaluation_run
                        SET finished_at = :finishedAt,
                            normalized_accuracy = :normalizedAccuracy,
                            mean_semantic_similarity = :meanSemanticSimilarity,
                            semantic_accuracy_at_080 = :semanticAccuracyAt080
                        WHERE id = :runId
                        """, Map.of(
                "runId", id,
                "finishedAt", finished,
                "normalizedAccuracy", normalizedAccuracy,
                "meanSemanticSimilarity", meanSemantic,
                "semanticAccuracyAt080", semanticAt080
        ));
    }

    public void insertResult(
            String id,
            String runId,
            String caseId,
            String predicted,
            boolean exact,
            boolean normalized,
            double semantic,
            boolean semanticPass,
            String retrievedChunksJson
    ) {
        jdbc.update("""
                        INSERT INTO evaluation_result (
                          id,
                          run_id,
                          case_id,
                          predicted_answer,
                          exact_match,
                          normalized_match,
                          semantic_similarity,
                          semantic_pass,
                          retrieved_chunks_json
                        ) VALUES (
                          :resultId,
                          :runId,
                          :caseId,
                          :predictedAnswer,
                          :exactMatch,
                          :normalizedMatch,
                          :semanticSimilarity,
                          :semanticPass,
                          CAST(:retrievedChunksJson AS jsonb)
                        )
                        """, Map.of(
                "resultId", id,
                "runId", runId,
                "caseId", caseId,
                "predictedAnswer", predicted,
                "exactMatch", exact,
                "normalizedMatch", normalized,
                "semanticSimilarity", semantic,
                "semanticPass", semanticPass,
                "retrievedChunksJson", retrievedChunksJson
        ));
    }

    public List<RunRow> listRuns() {
        return jdbc.query("""
                        SELECT id, dataset_id, started_at, finished_at, model_name, embedding_model_name,
                               normalized_accuracy, mean_semantic_similarity, semantic_accuracy_at_080
                        FROM evaluation_run ORDER BY started_at DESC
                        """, Map.of(), RUN_ROW_MAPPER);
    }

    public Optional<RunRow> findRun(String id) {
        List<RunRow> rows = jdbc.query("""
                        SELECT id, dataset_id, started_at, finished_at, model_name, embedding_model_name,
                               normalized_accuracy, mean_semantic_similarity, semantic_accuracy_at_080
                        FROM evaluation_run
                        WHERE id = :runId
                        """, Map.of("runId", id), RUN_ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<ResultRow> findResultsForRun(String runId) {
        return jdbc.query("""
                        SELECT ec.id AS case_id, ec.question, ec.ground_truth_answer,
                               er.predicted_answer, er.exact_match, er.normalized_match,
                               er.semantic_similarity, er.semantic_pass
                        FROM evaluation_result er
                        JOIN evaluation_case ec ON ec.id = er.case_id
                        WHERE er.run_id = :runId
                        ORDER BY ec.external_case_id
                        """, Map.of("runId", runId), (rs, rowNum) -> new ResultRow(
                        rs.getString("case_id"),
                        rs.getString("question"),
                        rs.getString("ground_truth_answer"),
                        rs.getString("predicted_answer"),
                        rs.getBoolean("exact_match"),
                        rs.getBoolean("normalized_match"),
                        rs.getBigDecimal("semantic_similarity"),
                        rs.getBoolean("semantic_pass")
                ));
    }

    public record EvalCaseRow(String id, String question, String groundTruth) {
    }

    public record RunRow(
            String id,
            String datasetId,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            String modelName,
            String embeddingModelName,
            BigDecimal normalizedAccuracy,
            BigDecimal meanSemanticSimilarity,
            BigDecimal semanticAccuracyAt080
    ) {
    }

    public record ResultRow(
            String caseId,
            String question,
            String groundTruth,
            String predicted,
            boolean exactMatch,
            boolean normalizedMatch,
            BigDecimal semanticSimilarity,
            boolean semanticPass
    ) {
    }
}
