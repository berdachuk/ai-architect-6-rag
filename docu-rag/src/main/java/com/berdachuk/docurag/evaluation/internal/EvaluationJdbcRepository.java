package com.berdachuk.docurag.evaluation.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
            rs.getBigDecimal("semantic_accuracy_at_threshold"),
            rs.getBigDecimal("semantic_pass_threshold"),
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

    public int countCasesByDatasetId(String datasetId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM evaluation_case WHERE dataset_id = :datasetId",
                Map.of("datasetId", datasetId),
                Integer.class
        );
        return count == null ? 0 : count;
    }

    @Transactional
    public void insertDatasetIfMissing(String id, String name, String version, String description) {
        jdbc.update("""
                        INSERT INTO evaluation_dataset (id, name, version, description)
                        VALUES (:id, :name, :version, :description)
                        ON CONFLICT (name) DO NOTHING
                        """,
                Map.of(
                        "id", id,
                        "name", name,
                        "version", version,
                        "description", description
                ));
    }

    @Transactional
    public void insertCase(
            String id,
            String datasetId,
            String externalCaseId,
            String question,
            String groundTruth,
            String category,
            String difficulty,
            String metadataJson
    ) {
        jdbc.update("""
                        INSERT INTO evaluation_case (
                          id,
                          dataset_id,
                          external_case_id,
                          question,
                          ground_truth_answer,
                          category,
                          difficulty,
                          metadata_json
                        ) VALUES (
                          :id,
                          :datasetId,
                          :externalCaseId,
                          :question,
                          :groundTruth,
                          :category,
                          :difficulty,
                          CAST(:metadataJson AS jsonb)
                        )
                        """,
                Map.of(
                        "id", id,
                        "datasetId", datasetId,
                        "externalCaseId", externalCaseId,
                        "question", question,
                        "groundTruth", groundTruth,
                        "category", category,
                        "difficulty", difficulty,
                        "metadataJson", metadataJson == null ? "{}" : metadataJson
                ));
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

    @Transactional
    public void insertRun(
            String id,
            String datasetId,
            OffsetDateTime started,
            String modelName,
            String embeddingModelName,
            double semanticPassThreshold
    ) {
        jdbc.update("""
                        INSERT INTO evaluation_run (
                          id,
                          dataset_id,
                          started_at,
                          model_name,
                          embedding_model_name,
                          semantic_pass_threshold
                        ) VALUES (
                          :runId,
                          :datasetId,
                          :startedAt,
                          :modelName,
                          :embeddingModelName,
                          :semanticPassThreshold
                        )
                        """, Map.of(
                "runId", id,
                "datasetId", datasetId,
                "startedAt", started,
                "modelName", modelName,
                "embeddingModelName", embeddingModelName,
                "semanticPassThreshold", semanticPassThreshold
        ));
    }

    @Transactional
    public void finishRun(
            String id,
            OffsetDateTime finished,
            double normalizedAccuracy,
            double meanSemantic,
            double semanticAtThreshold,
            double semanticAt080
    ) {
        jdbc.update("""
                        UPDATE evaluation_run
                        SET finished_at = :finishedAt,
                            normalized_accuracy = :normalizedAccuracy,
                            mean_semantic_similarity = :meanSemanticSimilarity,
                            semantic_accuracy_at_threshold = :semanticAccuracyAtThreshold,
                            semantic_accuracy_at_080 = :semanticAccuracyAt080
                        WHERE id = :runId
                        """, Map.of(
                "runId", id,
                "finishedAt", finished,
                "normalizedAccuracy", normalizedAccuracy,
                "meanSemanticSimilarity", meanSemantic,
                "semanticAccuracyAtThreshold", semanticAtThreshold,
                "semanticAccuracyAt080", semanticAt080
        ));
    }

    @Transactional
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
                               normalized_accuracy, mean_semantic_similarity, semantic_accuracy_at_threshold,
                               semantic_pass_threshold, semantic_accuracy_at_080
                        FROM evaluation_run ORDER BY started_at DESC
                        """, Map.of(), RUN_ROW_MAPPER);
    }

    public Optional<RunRow> findRun(String id) {
        List<RunRow> rows = jdbc.query("""
                        SELECT id, dataset_id, started_at, finished_at, model_name, embedding_model_name,
                               normalized_accuracy, mean_semantic_similarity, semantic_accuracy_at_threshold,
                               semantic_pass_threshold, semantic_accuracy_at_080
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
            BigDecimal semanticAccuracy,
            BigDecimal semanticPassThreshold,
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
