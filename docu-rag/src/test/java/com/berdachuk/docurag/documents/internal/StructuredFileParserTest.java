package com.berdachuk.docurag.documents.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredFileParserTest {

    private final StructuredFileParser parser = new StructuredFileParser(new ObjectMapper());

    @Test
    void parsesSinglePrettyPrintedJsonObject() throws Exception {
        Path tmp = Files.createTempFile("docurag-single-object", ".json");
        tmp.toFile().deleteOnExit();
        String json = """
                {
                  "id": "doc-1",
                  "title": "Single doc",
                  "category": "faq",
                  "source": "test",
                  "text": "This is long enough content for ingestion threshold checks."
                }
                """;
        Files.writeString(tmp, json, StandardCharsets.UTF_8);

        StructuredFileParser.ParsedFile result = parser.parseFile(tmp);

        assertThat(result.malformedRecordsSkipped()).isEqualTo(0);
        assertThat(result.documents()).hasSize(1);
        assertThat(result.documents().getFirst().externalId()).isEqualTo("doc-1");
        assertThat(result.documents().getFirst().text())
                .isEqualTo("This is long enough content for ingestion threshold checks.");
    }

    @Test
    void skipsMalformedJsonlLinesButParsesValidOnes() throws Exception {
        Path tmp = Files.createTempFile("docurag-jsonl", ".jsonl");
        tmp.toFile().deleteOnExit();
        String jsonl = """
                {"id":"ok-1","text":"First valid row with enough characters for parsing."}
                {
                {"id":"ok-2","text":"Second valid row with enough characters for parsing."}
                """;
        Files.writeString(tmp, jsonl, StandardCharsets.UTF_8);

        StructuredFileParser.ParsedFile result = parser.parseFile(tmp);

        assertThat(result.documents()).hasSize(2);
        assertThat(result.malformedRecordsSkipped()).isEqualTo(1);
        assertThat(result.documents().get(0).externalId()).isEqualTo("ok-1");
        assertThat(result.documents().get(1).externalId()).isEqualTo("ok-2");
    }
}
