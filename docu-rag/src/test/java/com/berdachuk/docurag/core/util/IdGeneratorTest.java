package com.berdachuk.docurag.core.util;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    @Test
    void generateId_format() {
        String id = IdGenerator.generateId();
        assertThat(IdGenerator.isValidId(id)).isTrue();
        assertThat(id).matches("[0-9a-f]{24}");
    }

    @RepeatedTest(50)
    void generateId_unique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            assertThat(ids.add(IdGenerator.generateId())).isTrue();
        }
    }

    @Test
    void isValidId_rejectsInvalid() {
        assertThat(IdGenerator.isValidId(null)).isFalse();
        assertThat(IdGenerator.isValidId("")).isFalse();
        assertThat(IdGenerator.isValidId("g".repeat(24))).isFalse();
        assertThat(IdGenerator.isValidId("a".repeat(23))).isFalse();
    }
}
