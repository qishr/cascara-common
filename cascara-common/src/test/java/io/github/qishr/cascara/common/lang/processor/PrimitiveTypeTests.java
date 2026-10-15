package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.lang.type.PrimitiveType;

public class PrimitiveTypeTests {
    @Test
    void test_GenericMessage() {
        GenericMessage message = GenericMessage.ERROR;
        PrimitiveType type = PrimitiveType.of(message);
        assertTrue(type.isScalar());
    }
}
