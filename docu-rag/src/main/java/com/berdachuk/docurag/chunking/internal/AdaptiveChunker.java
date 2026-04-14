package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.chunking.api.Chunker;

import java.util.List;
import java.util.regex.Pattern;

public class AdaptiveChunker implements Chunker {

    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\\n\\n+");
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?]\\s+");
    private static final Pattern PDF_LINE_BREAK = Pattern.compile("\\n\\n\\n+");

    private final SemanticChunker semanticChunker = new SemanticChunker();
    private final DocumentStructureChunker docStructureChunker = new DocumentStructureChunker();
    private final RecursiveCharacterChunker recursiveCharChunker = new RecursiveCharacterChunker();

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap, int minChars) {
        if (text == null || text.isBlank()) {
            return java.util.Collections.emptyList();
        }

        ContentAnalysis analysis = analyzeContent(text);
        Chunker delegate = selectChunker(analysis);
        return delegate.chunk(text, chunkSize, overlap, minChars);
    }

    @Override
    public String getName() {
        return "ADAPTIVE";
    }

    private ContentAnalysis analyzeContent(String text) {
        int paragraphBreaks = countMatches(PARAGRAPH_BREAK, text);
        int sentenceEnds = countMatches(SENTENCE_END, text);
        int newlineSeqs = countMatches(PDF_LINE_BREAK, text);
        int totalNewlines = (int) text.chars().filter(c -> c == '\n').count();

        boolean hasClearParagraphs = paragraphBreaks >= 2;
        boolean hasSentences = sentenceEnds >= 3;
        boolean isPdfLike = newlineSeqs > 0 || (totalNewlines > 100 && paragraphBreaks == 0);
        boolean isStructured = paragraphBreaks > sentenceEnds * 0.3;

        return new ContentAnalysis(paragraphBreaks, sentenceEnds, hasClearParagraphs, hasSentences, isPdfLike, isStructured);
    }

    private Chunker selectChunker(ContentAnalysis analysis) {
        if (analysis.isPdfLike() && analysis.hasClearParagraphs()) {
            return docStructureChunker;
        }

        if (analysis.hasSentences() && !analysis.hasClearParagraphs()) {
            return semanticChunker;
        }

        if (analysis.hasClearParagraphs() && analysis.hasSentences()) {
            return docStructureChunker;
        }

        if (analysis.hasClearParagraphs()) {
            return docStructureChunker;
        }

        if (analysis.hasSentences()) {
            return semanticChunker;
        }

        return recursiveCharChunker;
    }

    private int countMatches(Pattern pattern, String text) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private record ContentAnalysis(
            int paragraphBreaks,
            int sentenceEnds,
            boolean hasClearParagraphs,
            boolean hasSentences,
            boolean isPdfLike,
            boolean isStructured
    ) {}
}