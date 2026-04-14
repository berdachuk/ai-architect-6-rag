package com.berdachuk.docurag.chunking.internal;

import com.berdachuk.docurag.chunking.api.Chunker;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ChunkerFactory {

    private final Map<ChunkingStrategy, Chunker> chunkers = new EnumMap<>(ChunkingStrategy.class);

    public ChunkerFactory() {
        chunkers.put(ChunkingStrategy.ADAPTIVE, new AdaptiveChunker());
        chunkers.put(ChunkingStrategy.SEMANTIC, new SemanticChunker());
        chunkers.put(ChunkingStrategy.DOCUMENT_STRUCTURE, new DocumentStructureChunker());
        chunkers.put(ChunkingStrategy.RECURSIVE_CHARACTER, new RecursiveCharacterChunker());
        chunkers.put(ChunkingStrategy.CHARACTER, new RecursiveCharacterChunker());
    }

    public Chunker getChunker(ChunkingStrategy strategy) {
        return chunkers.getOrDefault(strategy, chunkers.get(ChunkingStrategy.ADAPTIVE));
    }

    public Chunker getChunker(String strategyName) {
        ChunkingStrategy strategy = ChunkingStrategy.fromString(strategyName);
        return getChunker(strategy);
    }

    public enum ChunkingStrategy {
        ADAPTIVE,
        SEMANTIC,
        DOCUMENT_STRUCTURE,
        RECURSIVE_CHARACTER,
        CHARACTER;

        public static ChunkingStrategy fromString(String name) {
            if (name == null || name.isBlank()) {
                return ADAPTIVE;
            }
            try {
                return ChunkingStrategy.valueOf(name.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                return ADAPTIVE;
            }
        }
    }
}