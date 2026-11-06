package io.github.qishr.cascara.test.common.junit.extension;

import org.opentest4j.AssertionFailedError;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class AssertReportedExtension extends AbstractDiagnosticExtension {
    public void error(String expectedMessage) {
        assertReported(expectedMessage, Level.ERROR);
    }

    public void warn(String expectedMessage) {
        assertReported(expectedMessage, Level.WARN);
    }

    public void info(String expectedMessage) {
        assertReported(expectedMessage, Level.INFO);
    }

    public void debug(String expectedMessage) {
        assertReported(expectedMessage, Level.DEBUG);
    }

    public void trace(String expectedMessage) {
        assertReported(expectedMessage, Level.TRACE);
    }

    public void errorContains(String expectedSubstring) {
        assertReportContains(expectedSubstring, Level.ERROR);
    }

    public void warnContains(String expectedSubstring) {
        assertReportContains(expectedSubstring, Level.WARN);
    }

    public void infoContains(String expectedSubstring) {
        assertReportContains(expectedSubstring, Level.INFO);
    }

    public void debugContains(String expectedMessage) {
        assertReportContains(expectedMessage, Level.DEBUG);
    }

    public void traceContains(String expectedSubstring) {
        assertReportContains(expectedSubstring, Level.TRACE);
    }

    private void assertReported(String message, Level level) {
        diagnostics.stream()
            .filter(e -> e.getLevel().equals(level))
            .filter(e -> e.getFormattedMessage().equals(message))
            .findAny()
            .orElseThrow(() -> new AssertionFailedError("Diagnostic message '" + message + "' with level '" + level +
                                                        "' was expected, but not reported."));
    }

    private void assertReportContains(String substring, Level level) {
        diagnostics.stream()
            .filter(e -> e.getLevel().equals(level))
            .filter(e -> e.getFormattedMessage().contains(substring))
            .findAny()
            .orElseThrow(() -> new AssertionFailedError("Diagnostic message containing '" + substring + "' with level '" + level +
                                                        "' was expected, but not reported."));
    }
}
