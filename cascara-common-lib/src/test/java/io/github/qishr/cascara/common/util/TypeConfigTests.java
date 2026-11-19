package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.type.TypeConfig;

public class TypeConfigTests {
    @Test
    void test_resolveJreClass() {
        TypeConfig config = TypeConfig.get();

        assertEquals("java.util.ArrayList", config.getMapping("java.util.List"));
    }
}
