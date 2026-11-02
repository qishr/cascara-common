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

public class AssertReportedExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
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
        assertLogged(expectedMessage, Level.ERROR);
    }

    public void warn(String expectedMessage) {
        assertLogged(expectedMessage, Level.WARN);
    }

    public void info(String expectedMessage) {
        assertLogged(expectedMessage, Level.INFO);
    }

    public void debug(String expectedMessage) {
        assertLogged(expectedMessage, Level.DEBUG);
    }

    public void trace(String expectedMessage) {
        assertLogged(expectedMessage, Level.TRACE);
    }

    private void assertLogged(String message, Level level) {
        diagnostics.stream()
            .filter(e -> e.getLevel().equals(level))
            .filter(e -> e.getFormattedMessage().equals(message))
            .findAny()
            .orElseThrow(() -> new AssertionFailedError("Log message '" + message + "' with level '" + level +
                                                        "' was expected, but not logged."));
    }
}
