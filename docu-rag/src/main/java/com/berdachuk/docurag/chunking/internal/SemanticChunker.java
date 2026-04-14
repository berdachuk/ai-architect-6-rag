package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.chunking.api.Chunker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class SemanticChunker implements Chunker {

    private static final String SENTENCE_SEPARATOR = "(?<=[.!?])\\s+";
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(SENTENCE_SEPARATOR);

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap, int minChars) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> sentences = splitIntoSentences(text);
        if (sentences.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int sentencesInCurrent = 0;

        for (String sentence : sentences) {
            int sentenceLen = sentence.length();
            int withSpace = current.length() + (current.length() > 0 ? 1 : 0) + sentenceLen;

            if (withSpace > chunkSize && current.length() > 0) {
                String chunk = current.toString().trim();
                if (chunk.length() >= minChars) {
                    chunks.add(chunk);
                }

                String overlapText = getOverlapText(current.toString(), overlap);
                current = new StringBuilder(overlapText);
                sentencesInCurrent = countSentences(overlapText);

                if (sentenceLen > chunkSize) {
                    List<String> subChunks = splitOversizedSentence(sentence, chunkSize, overlap, minChars);
                    chunks.addAll(subChunks.subList(0, subChunks.size() - 1));
                    if (!subChunks.isEmpty()) {
                        current = new StringBuilder(subChunks.get(subChunks.size() - 1));
                        sentencesInCurrent = 0;
                    }
                } else {
                    current.append(sentence).append(" ");
                    sentencesInCurrent = 1;
                }
            } else {
                if (current.length() > 0) {
                    current.append(" ");
                }
                current.append(sentence);
                sentencesInCurrent++;
            }
        }

        if (current.length() > 0) {
            String chunk = current.toString().trim();
            if (chunk.length() >= minChars) {
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    @Override
    public String getName() {
        return "SEMANTIC";
    }

    static List<String> splitIntoSentences(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String[] parts = SENTENCE_PATTERN.split(text);
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    static String getOverlapText(String chunk, int overlapChars) {
        if (overlapChars <= 0 || chunk.isEmpty()) {
            return "";
        }
        int start = Math.max(0, chunk.length() - overlapChars);
        String overlap = chunk.substring(start);
        int firstSpace = overlap.indexOf(' ');
        if (firstSpace > 0 && firstSpace < overlap.length() - 1) {
            return overlap.substring(firstSpace + 1);
        }
        return overlap;
    }

    private static int countSentences(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return SENTENCE_PATTERN.split(text).length;
    }

    private List<String> splitOversizedSentence(String sentence, int chunkSize, int overlap, int minChars) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int n = sentence.length();

        while (start < n) {
            int end = Math.min(n, start + chunkSize);
            String slice = sentence.substring(start, end).trim();

            if (slice.length() >= minChars || (result.isEmpty() && slice.length() > 0)) {
                result.add(slice);
            } else if (!result.isEmpty() && !slice.isEmpty()) {
                String last = result.remove(result.size() - 1);
                result.add((last + " " + slice).trim());
            }

            if (end >= n) {
                break;
            }

            start = end - overlap;
            if (start < 0) {
                start = 0;
            }
        }

        return result;
    }
}