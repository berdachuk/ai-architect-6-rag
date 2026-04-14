package com.berdachuk.docurag.chunking.api;

import java.util.List;

public interface Chunker {

    /**
     * Split text into semantic chunks.
     *
     * @param text       raw text content
     * @param chunkSize  max characters per chunk
     * @param overlap    characters to overlap between chunks
     * @param minChars   minimum chunk size
     * @return list of text chunks
     */
    List<String> chunk(String text, int chunkSize, int overlap, int minChars);

    /**
     * Human-readable name of this chunker strategy.
     */
    String getName();
}