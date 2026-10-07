package test.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.DiagnosticBuilder;
import io.github.qishr.cascara.common.diagnostic.StandardReporter;

public class DiagnosticSerializationTests extends SerializationTestBase {
    @Test
    void test_serializeStackTraceElement() {
        StackTraceElement original = new StackTraceElement("className1", "methodName1", "fileName1", 1);

        serializer.setReporter(new StandardReporter().setLevel(Level.DEBUG));

        String json = serializer.toString(original);

        REPORTER.debug(json);

        StackTraceElement ste = serializer.fromString(json, StackTraceElement.class);
        assertEquals("className1", ste.getClassName());
    }

    @Test
    void test_deserializeDiagnosticWithStackTrace() {

        StackTraceElement[] stackTrace = new StackTraceElement[]{
            new StackTraceElement("className1", "methodName1", "fileName1", 1)
        };

        Throwable cause = null;

        Diagnostic original = DiagnosticBuilder.build(
            "className",
            Level.WARN,
            stackTrace,
            cause,
            GenericMessage.WARN,
            "testWarning"
        );

        String json = serializer.toString(original);

        REPORTER.debug(json);

        Diagnostic diagnostic = serializer.fromString(json, Diagnostic.class);

        assertEquals(Level.WARN, diagnostic.getLevel());
        assertEquals("testWarning", diagnostic.getFormattedMessage());
        assertEquals("WARN-101", diagnostic.getMessage().getCode());
        assertEquals(1, diagnostic.getStackTrace().length);
        StackTraceElement element = diagnostic.getStackTrace()[0];
        assertEquals("className1", element.getClassName());
        assertEquals("methodName1", element.getMethodName());
    }
}
