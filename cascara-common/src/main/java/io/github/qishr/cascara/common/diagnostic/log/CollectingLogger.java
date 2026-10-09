package io.github.qishr.cascara.common.diagnostic.log;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;

@Experimental
public class CollectingLogger implements Logger {

    private final List<Diagnostic> diagnostics = new CopyOnWriteArrayList<>();

    @Override
    public void log(Diagnostic diagnostic) {
        diagnostics.add(diagnostic);
    }

    public void flushTo(Logger delegateLogger) {
        diagnostics.stream()
            .sorted(Comparator.comparing(Diagnostic::getTimestamp))
            .forEach(delegateLogger::log);
        delegateLogger.flush();
        diagnostics.clear();
    }

    @Override
    public void flush() {
        // Handled via explicit flushTo at end of test
    }
}