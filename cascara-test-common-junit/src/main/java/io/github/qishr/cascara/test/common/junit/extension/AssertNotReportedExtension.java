package io.github.qishr.cascara.test.common.junit.extension;

import org.opentest4j.AssertionFailedError;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class AssertNotReportedExtension extends AbstractDiagnosticExtension {
    public void error(String expectedMessage) {
        assertNotReported(expectedMessage, Level.ERROR);
    }

    public void warn(String expectedMessage) {
        assertNotReported(expectedMessage, Level.WARN);
    }

    public void info(String expectedMessage) {
        assertNotReported(expectedMessage, Level.INFO);
    }

    public void debug(String expectedMessage) {
        assertNotReported(expectedMessage, Level.DEBUG);
    }

    public void trace(String expectedMessage) {
        assertNotReported(expectedMessage, Level.TRACE);
    }

    public void errorContains(String expectedSubstring) {
        assertReportDoesNotContain(expectedSubstring, Level.ERROR);
    }

    public void warnContains(String expectedSubstring) {
        assertReportDoesNotContain(expectedSubstring, Level.WARN);
    }

    public void infoContains(String expectedSubstring) {
        assertReportDoesNotContain(expectedSubstring, Level.INFO);
    }

    public void debugContains(String expectedMessage) {
        assertReportDoesNotContain(expectedMessage, Level.DEBUG);
    }

    public void traceContains(String expectedSubstring) {
        assertReportDoesNotContain(expectedSubstring, Level.TRACE);
    }

    private void assertNotReported(String message, Level level) {
        for (Diagnostic d : diagnostics) {
            String s = d.getFormattedMessage();
            if (s.equals(message)) {
                throw new AssertionFailedError("Diagnostic message '" + message + "' with level '" + level +
                "' was unexpected, but was reported.");
            }
        }
    }

    private void assertReportDoesNotContain(String substring, Level level) {
        // System.err.println(diagnostics.size() + " diagnostic reports");
        for (Diagnostic d : diagnostics) {
            String s = d.getFormattedMessage();
            if (s.contains(substring)) {
                // System.err.println("******** " + s);
                new AssertionFailedError("Diagnostic message containing '" + substring + "' with level '" + level +
                                                        "' was unexpected, but was reported.");
            }
        }
    }
}
