package io.github.qishr.cascara.test.common.junit.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;

public class AbstractDiagnosticExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    protected final List<Diagnostic> diagnostics = new ArrayList<>();
    protected final Consumer<Diagnostic> consumer = (d) -> diagnostics.add(d);

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        GlobalReporter.get().addDiagnosticConsumer(consumer);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        GlobalReporter.get().removeDiagnosticConsumer(consumer);
        diagnostics.clear();
    }
}
