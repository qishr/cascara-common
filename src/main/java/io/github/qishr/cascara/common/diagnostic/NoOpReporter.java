package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class NoOpReporter implements Reporter {

    public NoOpReporter(ReportDiagnosticWriter logger) {
    }

    public NoOpReporter() {
        // Nothing to see here
    }

    @Override
    public NoOpReporter forClass(Class<?> clazz) {
        return this;
    }

    @Override
    public NoOpReporter setLevel(Level level) {
        return this;
    }

    @Override
    public NoOpReporter setStringWriter(ReportStringWriter writer) {
        return this;
    }

    @Override
    public NoOpReporter setDiagnosticWriter(ReportDiagnosticWriter logger) {
        return this;
    }

    @Override
    public NoOpReporter setCollector(ReportCollector collector) {
        return this;
    }

    public NoOpReporter setDisableSystemOutput(boolean b) {
        return this;
    }

    @Override
    public void trace(Object... msg) {}

    @Override
    public void debug(Object... msg) {}

    @Override
    public void info(Object... msg) {}

    @Override
    public void warn(Object... msg) {}

    @Override
    public void error(Object... msg) {}

    //
    // With Location
    //

    @Override
    public void infoAt(int line, int column, URI uri, Object... msg) {}

    @Override
    public void warnAt(int line, int column, URI uri, Object... msg) {}

    @Override
    public void errorAt(int line, int column, URI uri, Object... msg) {}
}
