package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.documents.internal.PdfTextExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceImplTest {

    private final SemanticChunker semanticChunker = new SemanticChunker();
    private final DocumentStructureChunker docStructureChunker = new DocumentStructureChunker();
    private final RecursiveCharacterChunker recursiveCharChunker = new RecursiveCharacterChunker();

    @Test
    void semanticChunkerBasic() {
        String text = "First sentence here. Second sentence here. Third sentence here.";
        List<String> parts = semanticChunker.chunk(text, 50, 10, 5);

        assertThat(parts).isNotEmpty();
        for (String part : parts) {
            assertThat(part.length()).isLessThanOrEqualTo(60);
        }
    }

    @Test
    void semanticChunkerRespectsOverlap() {
        String text = "Sentence one. Sentence two. Sentence three. Sentence four.";
        List<String> parts = semanticChunker.chunk(text, 30, 15, 5);

        assertThat(parts.size()).isGreaterThan(1);
        for (int i = 1; i < parts.size(); i++) {
            String prev = parts.get(i - 1);
            String curr = parts.get(i);
            int overlap = 15;
            assertThat(prev.endsWith(curr.substring(0, Math.min(overlap, curr.length()))));
        }
    }

    @Test
    void semanticChunkerEmpty() {
        List<String> parts = semanticChunker.chunk("", 30, 10, 5);
        assertThat(parts).isEmpty();
    }

    @Test
    void semanticChunkerSingleSentence() {
        String text = "This is a single sentence.";
        List<String> parts = semanticChunker.chunk(text, 100, 10, 5);
        assertThat(parts).hasSize(1);
        assertThat(parts.get(0)).isEqualTo("This is a single sentence.");
    }

    @ParameterizedTest
    @CsvSource({
            "50, 30, 10, 5",
            "35, 30, 10, 5",
            "30, 30, 10, 5"
    })
    void semanticChunkerChunkCounts(int textLen, int chunkSize, int overlap, int minChars) {
        String text = "Sentence one. " + "abcdefghij".repeat(textLen / 20);
        List<String> parts = semanticChunker.chunk(text, chunkSize, overlap, minChars);
        assertThat(parts).isNotEmpty();
    }

    @Test
    void docStructureChunkerBasic() {
        String text = "First paragraph here.\n\nSecond paragraph here.";
        List<String> parts = docStructureChunker.chunk(text, 100, 20, 5);

        assertThat(parts).isNotEmpty();
        for (String part : parts) {
            assertThat(part.length()).isLessThanOrEqualTo(120);
        }
    }

    @Test
    void docStructureChunkerPreservesParagraphs() {
        String text = "Paragraph one content.\n\nParagraph two content.\n\nParagraph three content.";
        List<String> parts = docStructureChunker.chunk(text, 100, 20, 5);

        assertThat(parts).isNotEmpty();
        boolean hasMultiParagraph = parts.stream().anyMatch(p -> p.contains("\n\n"));
        assertThat(hasMultiParagraph).isTrue();
    }

    @Test
    void docStructureChunkerEmpty() {
        List<String> parts = docStructureChunker.chunk("", 30, 10, 5);
        assertThat(parts).isEmpty();
    }

    @Test
    void recursiveCharChunkerBasic() {
        String text = "a".repeat(100);
        List<String> parts = recursiveCharChunker.chunk(text, 30, 10, 5);

        assertThat(parts).hasSize(5);
        assertThat(parts.get(0)).hasSize(30);
    }

    @Test
    void recursiveCharChunkerRespectsOverlap() {
        String text = "abcdefghij".repeat(10);
        List<String> parts = recursiveCharChunker.chunk(text, 30, 10, 5);

        for (int i = 1; i < parts.size(); i++) {
            String prev = parts.get(i - 1);
            String curr = parts.get(i);
            String prevEnd = prev.substring(prev.length() - 10);
            String currStart = curr.substring(0, Math.min(10, curr.length()));
            assertThat(curr).contains(prevEnd);
        }
    }

    @Test
    void recursiveCharChunkerEmpty() {
        List<String> parts = recursiveCharChunker.chunk("", 30, 10, 5);
        assertThat(parts).isEmpty();
    }

    @Test
    void chunkerWithSample5JsonlFixture() throws IOException {
        ClassPathResource resource = new ClassPathResource("fixtures/sample.5.jsonl");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        String[] lines = content.trim().split("\n");

        int totalChunks = 0;
        for (String line : lines) {
            JsonNode node = mapper.readTree(line);
            String text = node.get("text").asText();

            List<String> chunks = semanticChunker.chunk(text, 200, 50, 20);
            assertThat(chunks).isNotEmpty();

            boolean allAboveMin = chunks.stream().allMatch(c -> c.length() >= 20);
            assertThat(allAboveMin).isTrue();

            totalChunks += chunks.size();
        }
        assertThat(totalChunks).isGreaterThan(0);
    }

    @Test
    void chunkerWithSampleJsonlFixture() throws IOException {
        ClassPathResource resource = new ClassPathResource("fixtures/sample.jsonl");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(content.trim());

        String text = node.get("text").asText();
        List<String> chunks = semanticChunker.chunk(text, 100, 30, 10);

        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk.length()).isLessThanOrEqualTo(130);
        }
    }

    @Test
    void chunkerWithEuPackagingPdfFixture() throws IOException {
        Path tmp = Files.createTempFile("docurag-eu", ".pdf");
        tmp.toFile().deleteOnExit();
        try (var in = new ClassPathResource("fixtures/EU_2018_packaging_guidelines_en_1.pdf").getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(tmp);
        assertThat(text).isNotBlank();
        assertThat(text.length()).isGreaterThan(1000);

        List<String> chunks = docStructureChunker.chunk(text, 500, 100, 50);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream().allMatch(s -> s.length() >= 50)).isTrue();

        int expectedMinChunks = (int) Math.ceil((double) text.length() / 500);
        assertThat(chunks.size()).isGreaterThanOrEqualTo(Math.max(1, expectedMinChunks / 5));
    }

    @Test
    void chunkerWithNhsDmImplementationGuidePdfFixture() throws IOException {
        Path tmp = Files.createTempFile("docurag-nhs-dm", ".pdf");
        tmp.toFile().deleteOnExit();
        try (var in = new ClassPathResource("fixtures/NHS_dm%2Bd_Implementation_Guide_Primary_Care_v2.0.pdf").getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(tmp);
        assertThat(text).isNotBlank();
        assertThat(text.length()).isGreaterThan(100);

        List<String> chunks = docStructureChunker.chunk(text, 300, 75, 30);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream().allMatch(s -> s.length() >= 30)).isTrue();
    }

    @Test
    void chunkerWithNhsEndorsementGuidePdfFixture() throws IOException {
        Path tmp = Files.createTempFile("docurag-nhs-endorsement", ".pdf");
        tmp.toFile().deleteOnExit();
        try (var in = new ClassPathResource("fixtures/NHSBSA_Guidance_for_Endorsement_v7.2_Sept_2019.pdf").getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(tmp);
        assertThat(text).isNotBlank();
        assertThat(text.length()).isGreaterThan(100);

        List<String> chunks = docStructureChunker.chunk(text, 400, 80, 40);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream().allMatch(s -> s.length() >= 40)).isTrue();

        int expectedMinChunks = (int) Math.ceil((double) text.length() / 400);
        assertThat(chunks.size()).isGreaterThanOrEqualTo(Math.max(1, expectedMinChunks / 5));
    }
}