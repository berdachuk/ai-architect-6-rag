package com.berdachuk.docurag.evaluation.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.berdachuk.docurag.core.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationSampleDatasetSeeder {

    static final String DATASET_NAME = "medical-rag-eval-v1";
    private static final String DATASET_VERSION = "1";
    private static final String DATASET_DESCRIPTION = "Seeded from classpath:evaluation/medical-rag-corpus.sample.500.jsonl";
    private static final String DATASET_RESOURCE_PATH = "evaluation/medical-rag-corpus.sample.500.jsonl";
    private static final int MAX_CASES = 50;

    private final EvaluationJdbcRepository repository;
    private final ObjectMapper objectMapper;

    public void ensureDatasetSeeded(String datasetName) {
        if (!DATASET_NAME.equals(datasetName)) {
            return;
        }

        Optional<String> existingDatasetId = repository.findDatasetIdByName(DATASET_NAME);
        if (existingDatasetId.isPresent() && repository.countCasesByDatasetId(existingDatasetId.get()) > 0) {
            return;
        }

        String datasetId = existingDatasetId.orElseGet(IdGenerator::generateId);
        repository.insertDatasetIfMissing(datasetId, DATASET_NAME, DATASET_VERSION, DATASET_DESCRIPTION);
        String resolvedDatasetId = repository.findDatasetIdByName(DATASET_NAME)
                .orElseThrow(() -> new IllegalStateException("Evaluation dataset is missing: " + DATASET_NAME));
        if (repository.countCasesByDatasetId(resolvedDatasetId) > 0) {
            return;
        }

        int inserted = seedCases(resolvedDatasetId);
        log.info("Seeded evaluation dataset '{}' with {} cases from {}", DATASET_NAME, inserted, DATASET_RESOURCE_PATH);
    }

    private int seedCases(String datasetId) {
        Resource resource = new ClassPathResource(DATASET_RESOURCE_PATH);
        if (!resource.exists()) {
            throw new IllegalStateException("Evaluation sample dataset not found: " + DATASET_RESOURCE_PATH);
        }

        int inserted = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null && inserted < MAX_CASES) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                JsonNode row = objectMapper.readTree(line);
                JsonNode meta = row.path("meta_json");
                if (!meta.isObject()) {
                    continue;
                }
                String question = textOrNull(meta, "question");
                String answer = textOrNull(meta, "answer");
                if (question == null || answer == null) {
                    continue;
                }

                ObjectNode metadata = objectMapper.createObjectNode();
                putIfNotBlank(metadata, "doc_id", textOrNull(row, "doc_id"));
                putIfNotBlank(metadata, "source", textOrNull(row, "source"));
                putIfNotBlank(metadata, "title", textOrNull(row, "title"));
                metadata.put("source_line", lineNumber);

                repository.insertCase(
                        IdGenerator.generateId(),
                        datasetId,
                        "sample-" + String.format("%03d", inserted + 1),
                        question,
                        answer,
                        textOrNull(row, "category"),
                        null,
                        metadata.toString()
                );
                inserted++;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to seed evaluation dataset from " + DATASET_RESOURCE_PATH, e);
        }

        if (inserted == 0) {
            throw new IllegalStateException("No evaluation cases were extracted from " + DATASET_RESOURCE_PATH);
        }
        return inserted;
    }

    private static String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private static void putIfNotBlank(ObjectNode target, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            target.put(fieldName, value);
        }
    }
}
