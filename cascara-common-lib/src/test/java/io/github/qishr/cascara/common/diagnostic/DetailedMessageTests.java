package io.github.qishr.cascara.common.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableInstantiationException;

public class DetailedMessageTests {
    @Test
    void test_instantiation_noSuchMethod() {
        NoSuchMethodError cause = new NoSuchMethodError("com.example.Thing.getValue()");
        LocalizableInstantiationException e = new LocalizableInstantiationException(cause);
        assertEquals("No such method: com.example.Thing.getValue()", e.getLocalizedMessage());
    }
}
