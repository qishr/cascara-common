package io.github.qishr.cascara.test.common.junit.extension;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.AssertionFailedError;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;

public class AssertNotReportedExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        GlobalReporter.globalInstance().setDiagnosticConsumer(d -> diagnostics.add(d));
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        diagnostics.clear();
    }

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
        diagnostics.stream()
            .filter(e -> e.getLevel().equals(level))
            .filter(e -> !e.getFormattedMessage().equals(message))
            .findAny()
            .orElseThrow(() -> new AssertionFailedError("Diagnostic report message '" + message + "' with level '" + level +
                                                        "' was unexpected, but was reported."));
    }

    private void assertReportDoesNotContain(String substring, Level level) {
        diagnostics.stream()
            .filter(e -> e.getLevel().equals(level))
            .filter(e -> !e.getFormattedMessage().contains(substring))
            .findAny()
            .orElseThrow(() -> new AssertionFailedError("Diagnostic report message containing '" + substring + "' with level '" + level +
                                                        "' was unexpected, but was reported."));
    }
}
