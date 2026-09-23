package io.github.qishr.cascara.common.test.diagnostic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.UnimplementedMethodException;

public class ExceptionTests {
    @Test
    void test_unimplementedMethodException() {
        UnimplementedMethodException ume = new UnimplementedMethodException();

        Object[] details = ume.getDetails();
        assertEquals(2, details.length);

        String callingClass = (String) details[0];
        String callingMethod = (String) details[1];

        assertEquals(ExceptionTests.class.getName(), callingClass);
        assertEquals("test_unimplementedMethodException", callingMethod);
    }
}
