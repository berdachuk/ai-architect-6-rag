package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.chunking.api.Chunker;

import java.util.ArrayList;
import java.util.List;

public class RecursiveCharacterChunker implements Chunker {

    @Override
    public List<String> chunk(String text, int chunkSize, int overlap, int minChars) {
        return charChunkWithOverlap(text, chunkSize, overlap, minChars);
    }

    @Override
    public String getName() {
        return "RECURSIVE_CHARACTER";
    }

    public static List<String> recursiveSplit(String text, int chunkSize, int minChars) {
        return charChunkWithOverlap(text, chunkSize, 0, minChars);
    }

    private static List<String> charChunkWithOverlap(String text, int chunkSize, int overlap, int minChars) {
        List<String> out = new ArrayList<>();
        int start = 0;
        int n = text.length();

        while (start < n) {
            int end = Math.min(n, start + chunkSize);
            String slice = text.substring(start, end);

            if (slice.length() >= minChars || (out.isEmpty() && slice.length() > 0)) {
                out.add(slice);
            } else if (!out.isEmpty() && !slice.isEmpty()) {
                String last = out.remove(out.size() - 1);
                out.add(last + slice);
            }

            if (end >= n) {
                break;
            }

            start = end - overlap;
            if (start < 0) {
                start = 0;
            }
        }

        return out;
    }
}