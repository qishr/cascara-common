package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.token.Token;

public class NoOpReporter implements Reporter {

    public NoOpReporter(Consumer<String> writer) {
    }

    public NoOpReporter() {
        // Nothing to see here
    }

    @Override
    public boolean collectsProblems() {
        return false;
    }

    @Override
    public NoOpReporter setLevel(Level level) {
        return this;
    }

    @Override
    public NoOpReporter setDiagnosticCollector(Consumer<Diagnostic> diagnosticCollector) {
        return this;
    }

    @Override
    public NoOpReporter setProblemCollector(Consumer<Diagnostic> diagnosticCollector) {
        return this;
    }

    public NoOpReporter setDisableSystemOutput(boolean b) {
        return this;
    }

    //
    // Plain
    //

    @Override
    public void trace(String format, Object... args) {}

    @Override
    public void debug(String format, Object... args) {}

    @Override
    public void info(String format, Object... args) {}

    @Override
    public void warn(String format, Object... args) {}

    @Override
    public void error(String format, Object... args) {}

    //
    // With Location
    //

    @Override
    public void infoAt(int start, int end, int line, int column, URI uri, String format, Object... args) {}

    @Override
    public void warnAt(int start, int end, int line, int column, URI uri, String format, Object... args) {}

    @Override
    public void errorAt(int start, int end, int line, int column, URI uri, String format, Object... args) {}

    //
    // With Token
    //

    @Override
    public void infoAt(Token token, URI uri, String format, Object... args) {}

    @Override
    public void warnAt(Token token, URI uri, String format, Object... args) {}

    @Override
    public void errorAt(Token token, URI uri, String format, Object... args) {}

}
