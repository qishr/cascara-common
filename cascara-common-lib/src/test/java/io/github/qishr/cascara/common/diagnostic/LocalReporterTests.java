package io.github.qishr.cascara.common.diagnostic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.LocalReporter;

public class LocalReporterTests {
    @Test
    void test_reportHasLevel() {
        LocalReporter reporter = new LocalReporter();
        reporter.setLevel(Level.DEBUG);

        AtomicBoolean lineHasLevel = new AtomicBoolean();
        reporter.setLineConsumer(line -> {
            if (line.contains("DEBUG")) {
                lineHasLevel.set(true);
            }
        });

        reporter.debug("test");
        assertTrue(lineHasLevel.get());
    }

    @Test
    void test_reportHasStackTrace() {
        AtomicBoolean lineHasException = new AtomicBoolean();
        AtomicBoolean lineHasStackTrace = new AtomicBoolean();

        LocalReporter reporter = new LocalReporter()
            .setStackTraceEnabled(true)
            .setSystemOutputEnabled(false)
            .setLineConsumer(line -> {
                if (line.contains("قشر")) {
                    lineHasException.set(true);
                }
                if (line.contains(".java")) {
                    lineHasStackTrace.set(true);
                }
            }
        );

        Throwable cause = new IllegalAccessError("قشر");
        reporter.error(cause, GenericMessage.ERROR, cause.getMessage());

        assertTrue(lineHasException.get());
        assertTrue(lineHasStackTrace.get());
    }

    @Test
    void testFormatter() {
        LocalReporter r = new LocalReporter().setLevel(Level.DEBUG);

        r.setPrefixEveryLine(true);
        r.debug("One");
        r.debug("  Two");
        r.debug("Split1\n  Split2");

        // r.setPrefixEveryLine(false);
        // r.debug("One");
        // r.debug("  Two");
        // r.debug("Split1\n  Split2");
    }
}
