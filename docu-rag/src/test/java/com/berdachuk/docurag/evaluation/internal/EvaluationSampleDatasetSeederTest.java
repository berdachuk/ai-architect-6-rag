package com.berdachuk.docurag.evaluation.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationSampleDatasetSeederTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EvaluationSampleDatasetSeeder seeder =
            new EvaluationSampleDatasetSeeder(null, objectMapper);

    @Test
    void parsesMetaJsonWhenDatasetStoresItAsEscapedJsonString() throws Exception {
        var row = objectMapper.readTree("""
                {
                  "meta_json": "{\\"question\\":\\"What is hypertension?\\",\\"answer\\":\\"High blood pressure.\\"}"
                }
                """);

        var meta = seeder.parseMeta(row.path("meta_json"));

        assertThat(meta.isObject()).isTrue();
        assertThat(meta.path("question").asText()).isEqualTo("What is hypertension?");
        assertThat(meta.path("answer").asText()).isEqualTo("High blood pressure.");
    }

    @Test
    void keepsMetaJsonWhenItIsAlreadyAnObject() throws Exception {
        var row = objectMapper.readTree("""
                {
                  "meta_json": {
                    "question": "What is hypertension?",
                    "answer": "High blood pressure."
                  }
                }
                """);

        var meta = seeder.parseMeta(row.path("meta_json"));

        assertThat(meta.isObject()).isTrue();
        assertThat(meta.path("question").asText()).isEqualTo("What is hypertension?");
        assertThat(meta.path("answer").asText()).isEqualTo("High blood pressure.");
    }
}
