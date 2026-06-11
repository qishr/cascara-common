package io.github.qishr.cascara.common.diagnostic;

import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.lang.token.Token;

public class SilentErrorTracker implements Reporter {

    private boolean hasErrors;

    public SilentErrorTracker() {
        // Nothing to see here
    }

    public boolean hasErrors() { return hasErrors; }

    @Override
    public boolean collectsProblems() {
        // Returning true here means the parser won't throw exceptions
        return true;
    }

    @Override
    public SilentErrorTracker setLevel(Level level) {
        return this;
    }

    @Override
    public SilentErrorTracker setDiagnosticCollector(Consumer<Diagnostic> diagnosticCollector) {
        return this;
    }

    @Override
    public SilentErrorTracker setProblemCollector(Consumer<Diagnostic> diagnosticCollector) {
        return this;
    }

    //
    // Exception
    //

    @Override
    public void error(LocalizableException e) {
        hasErrors = true;
    }

    @Override
    public void error(LocalizableRuntimeException e) {
        hasErrors = true;
    }

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
    public void error(DiagnosticCode code, Object... args) {
        hasErrors = true;
    }

    @Override
    public void error(Throwable cause, DiagnosticCode code, Object... args) {
        hasErrors = true;
    }

    //
    // With Location
    //

    @Override
    public void infoAt(int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(int line, int column, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, DiagnosticCode code, Object... args) {
        hasErrors = true;
    }

    @Override
    public void errorAt(int line, int column, Throwable cause, DiagnosticCode code, Object... args) {
        hasErrors = true;
    }

    //
    // With Location invluding offset
    //

    @Override
    public void infoAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(int line, int column, int start, int end, DiagnosticCode code, Object... args) {
        hasErrors = true;
    }

    @Override
    public void errorAt(int line, int column, int start, int end, Throwable cause, DiagnosticCode code, Object... args) {
        hasErrors = true;
    }

    //
    // With Token
    //

    @Override
    public void infoAt(Token token, DiagnosticCode code, Object... args) {}

    @Override
    public void warnAt(Token token, DiagnosticCode code, Object... args) {}

    @Override
    public void errorAt(Token token, DiagnosticCode code, Object... args) {
        hasErrors = true;
    }

    @Override
    public void errorAt(Token token, Throwable cause, DiagnosticCode code, Object... args) {
        hasErrors = true;
    }
}
