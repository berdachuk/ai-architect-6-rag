package com.berdachuk.docurag.chunking.internal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceImplTest {

    @Test
    void characterChunksRespectsOverlap() {
        String text = "a".repeat(100);
        List<String> parts = ChunkingServiceImpl.characterChunks(text, 30, 10, 5);
        assertThat(parts).isNotEmpty();
        assertThat(parts.stream().allMatch(s -> s.length() <= 35)).isTrue();
    }
}
