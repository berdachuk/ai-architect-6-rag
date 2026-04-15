package com.berdachuk.docurag.chunking.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticChunkerTest {

    private final SemanticChunker chunker = new SemanticChunker();

    @Test
    void splitsIntoSentences() {
        String text = "This is the first sentence. Here is the second sentence! And a third?";
        List<String> sentences = SemanticChunker.splitIntoSentences(text);

        assertThat(sentences).hasSize(3);
        assertThat(sentences.get(0)).isEqualTo("This is the first sentence.");
        assertThat(sentences.get(1)).isEqualTo("Here is the second sentence!");
        assertThat(sentences.get(2)).isEqualTo("And a third?");
    }

    @Test
    void chunkRespectsSentenceBoundaries() {
        String text = "First sentence here. Second sentence here. Third sentence here.";
        List<String> chunks = chunker.chunk(text, 50, 10, 5);

        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk).doesNotContain(" . ");
        }
    }

    @Test
    void chunkRespectsMaxSize() {
        String text = "Short sentence. Another sentence. A third one here.";
        List<String> chunks = chunker.chunk(text, 30, 5, 5);

        for (String chunk : chunks) {
            assertThat(chunk.length()).isLessThanOrEqualTo(30);
        }
    }

    @Test
    void handlesEmptyText() {
        List<String> chunks = chunker.chunk("", 50, 10, 5);
        assertThat(chunks).isEmpty();
    }

    @Test
    void handlesSingleSentence() {
        String text = "This is a single sentence.";
        List<String> chunks = chunker.chunk(text, 100, 10, 5);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("This is a single sentence.");
    }

    @Test
    void overlapCarriesSentence() {
        String text = "Sentence one here. Sentence two here. Sentence three here.";
        List<String> chunks = chunker.chunk(text, 25, 15, 5);

        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void chunkWithRepetitiveTextWithoutSentenceBreaks() {
        String text = "Sentence one here. " + "word ".repeat(30) + "ends here.";
        List<String> chunks = chunker.chunk(text, 50, 10, 5);

        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            assertThat(chunk.length()).isLessThanOrEqualTo(60);
        }
    }

    @Test
    void emptyTextReturnsEmptyList() {
        List<String> chunks = chunker.chunk("", 50, 10, 5);
        assertThat(chunks).isEmpty();
    }

    @Test
    void nullTextReturnsEmptyList() {
        List<String> chunks = chunker.chunk(null, 50, 10, 5);
        assertThat(chunks).isEmpty();
    }

    @Test
    void getNameReturnsSemantic() {
        assertThat(chunker.getName()).isEqualTo("SEMANTIC");
    }
}