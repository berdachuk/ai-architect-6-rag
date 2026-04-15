package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.documents.internal.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveChunkerTest {

    private final AdaptiveChunker chunker = new AdaptiveChunker();

    @Test
    void selectsSemanticForPlainText() {
        String text = "This is sentence one. This is sentence two. This is sentence three. Another sentence here.";
        List<String> chunks = chunker.chunk(text, 50, 10, 5);

        assertThat(chunks).isNotEmpty();
        assertThat(chunker.getName()).isEqualTo("ADAPTIVE");
    }

    @Test
    void selectsDocumentStructureForParagraphs() {
        String text = "First paragraph content here.\n\nSecond paragraph content.\n\nThird paragraph with more text.";
        List<String> chunks = chunker.chunk(text, 100, 20, 5);

        assertThat(chunks).isNotEmpty();
    }

    @Test
    void handlesPdfContent() throws Exception {
        Path tmp = Files.createTempFile("docurag-adaptive", ".pdf");
        tmp.toFile().deleteOnExit();
        try (var in = new ClassPathResource("fixtures/EU_2018_packaging_guidelines_en_1.pdf").getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(tmp);
        assertThat(text).isNotBlank();

        List<String> chunks = chunker.chunk(text, 500, 100, 50);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.stream().allMatch(s -> s.length() >= 50)).isTrue();
    }

    @Test
    void handlesEmptyText() {
        List<String> chunks = chunker.chunk("", 50, 10, 5);
        assertThat(chunks).isEmpty();
    }

    @Test
    void handlesNullText() {
        List<String> chunks = chunker.chunk(null, 50, 10, 5);
        assertThat(chunks).isEmpty();
    }

    @Test
    void handlesShortText() {
        String text = "Short text.";
        List<String> chunks = chunker.chunk(text, 100, 10, 5);
        assertThat(chunks).hasSize(1);
    }

    @Test
    void getNameReturnsAdaptive() {
        assertThat(chunker.getName()).isEqualTo("ADAPTIVE");
    }
}