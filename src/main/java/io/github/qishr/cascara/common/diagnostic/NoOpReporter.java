package io.github.qishr.cascara.common.diagnostic;

import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
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
    // Exception
    //

    @Override
    public void error(LocalizableException e) {}

    @Override
    public void error(LocalizableRuntimeException e) {}

    //
    // Plain
    //

    @Override
    public void trace(String format, Object... args) {}

    @Override
    public void debug(String format, Object... args) {}

    @Override
    public void info(DiagnosticCode code, Object... args) {}

    @Override
    public void warn(DiagnosticCode code, Object... args) {}

    @Override
    public void error(DiagnosticCode code, Object... args) {}

    @Override
    public void error(Throwable cause, DiagnosticCode code, Object... args) {}

    //
    // With Location
    //

    @Override
    public void infoAt(int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, Throwable cause, DiagnosticCode code, Object... args) {}

    //
    // With Location invluding offset
    //

    @Override
    public void infoAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, int start, int end, Throwable cause, DiagnosticCode code, Object... args) {}

    //
    // With Token
    //

    @Override
    public void infoAt(Token token, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(Token token, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(Token token, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(Token token, Throwable cause, DiagnosticCode code, Object... args) {}

}
