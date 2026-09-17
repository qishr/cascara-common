package io.github.qishr.cascara.common.lang;

import org.junit.jupiter.api.BeforeEach;

import io.github.qishr.cascara.common.diagnostic.StandardReporter;

public abstract class SerializerTestBase {
    protected SerializerImpl serializer;

    @BeforeEach
    void setup() {
        serializer = new SerializerImpl();
        serializer.setReporter(new StandardReporter());
    }
}
