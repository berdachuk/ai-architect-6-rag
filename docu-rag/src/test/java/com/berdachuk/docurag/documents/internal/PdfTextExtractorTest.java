package com.berdachuk.docurag.documents.internal;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextExtractorTest {

    private static final int MIN_TEXT_LEN = 20;

    @Test
    void extractsTextFromClasspathFixtureAboveIngestMinimum() throws Exception {
        Path tmp = Files.createTempFile("docurag-tiny", ".pdf");
        tmp.toFile().deleteOnExit();
        try (var in = new ClassPathResource("fixtures/tiny.pdf").getInputStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extractText(tmp).trim();
        assertThat(text.length()).isGreaterThanOrEqualTo(MIN_TEXT_LEN);
    }
}
