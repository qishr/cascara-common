package io.github.qishr.cascara.common.diagnostic;

import java.io.PrintStream;
import java.net.URI;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.lang.token.Token;

public abstract class AbstractReporter<T extends AbstractReporter<?>> implements Reporter {
    protected Level level = Level.INFO;

    protected String source;

    /// Consumes diagnostics included in the current Level or more
    /// important, with ERROR being the most important.
    protected Consumer<Diagnostic> diagnosticCollector;

    /// Consumes ERROR, WARN, and INFO diagnostics.
    protected Consumer<Diagnostic> problemCollector;

    protected Consumer<String> stringWriter;

    protected boolean disableSystemOutput = false;
    protected boolean disableFlush = true;
    protected boolean printStackTrace;

    protected AbstractReporter(Consumer<String> writer) {
        this.stringWriter = writer;
    }

    protected AbstractReporter() {
        // Nothing to see here
    }

    protected abstract T self();

    /// {@inheritDoc}
    @Override
    public boolean collectsProblems() {
        return problemCollector != null;
    }

    /// {@inheritDoc}
    @Override
    public T setLevel(Level level) {
        this.level = level;
        return self();
    }

    /// {@inheritDoc}
    @Override
    public T setDiagnosticCollector(Consumer<Diagnostic> collector) {
        diagnosticCollector = collector;
        return self();
    }

    /// {@inheritDoc}
    @Override
    public T setProblemCollector(Consumer<Diagnostic> collector) {
        problemCollector = collector;
        return self();
    }

    public T setDisableSystemOutput(boolean b) {
        disableSystemOutput = b;
        return self();
    }

    public T setDisableFlush(boolean b) {
        disableFlush = b;
        return self();
    }

    public T setPrintStackTrace(boolean b) {
        printStackTrace = b;
        return self();
    }

    //
    // Exception
    //

    /// {@inheritDoc}
    @Override
    public void error(LocalizableException e) {
        report(buildDiagnostic(source, Level.ERROR, e.getCause(), e.getCode(), e.getDetails()));
    }

    /// {@inheritDoc}
    @Override
    public void error(LocalizableRuntimeException e) {
        if (e instanceof LocatableException locatable) {
            report(buildDiagnostic(
                locatable.getUri(),
                locatable.getLine(),
                locatable.getColumn(),
                LocatableException.UNKNOWN_COORD,
                LocatableException.UNKNOWN_COORD,
                source, Level.ERROR, e.getCause(), e.getCode(), e.getDetails()));
        } else {
            report(buildDiagnostic(source, Level.ERROR, e.getCause(), e.getCode(), e.getDetails()));
        }
    }

    //
    // Plain
    //

    /// {@inheritDoc}
    @Override
    public void trace(String message, Object... details) {
        report(buildDiagnostic(source, Level.TRACE, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void debug(String message, Object... details) {
        report(buildDiagnostic(source, Level.DEBUG, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void info(DiagnosticCode code, Object... details) {
        report(buildDiagnostic(source, Level.INFO, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void warn(DiagnosticCode code, Object... details) {
        report(buildDiagnostic(source, Level.WARN, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void error(DiagnosticCode code, Object... details) {
        report(buildDiagnostic(source, Level.ERROR, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void error(Throwable cause, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(source, Level.ERROR, cause, code, details));
    }

    //
    // With Location
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(int line, int column, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            null, line, column,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, Level.INFO, null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(int line, int column, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            null, line, column,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, Level.WARN, null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            null, line, column,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, Level.ERROR, null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, Throwable cause, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            null, line, column,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, Level.ERROR, cause, code, details
        ));
    }

    //
    // With Location Including Offset
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(int line, int column, int startOffset, int endOffset, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(null, line, column, startOffset, endOffset, source, Level.INFO, null, code, null, details));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(int line, int column, int startOffset, int endOffset, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(null, line, column, startOffset, endOffset, source, Level.WARN, null, code, null, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, int startOffset, int endOffset, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(null, line, column, startOffset, endOffset, source, Level.ERROR, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, int startOffset, int endOffset, Throwable cause, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(null, line, column, startOffset, endOffset, source, Level.ERROR, cause, code, details));
    }

    //
    // With Token
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(Token token, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(token, source, Level.INFO, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(Token token, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(token, source, Level.WARN, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(Token token, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(token, source, Level.ERROR, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(Token token, Throwable cause, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(token, source, Level.ERROR, cause, code, details));
    }

    //
    //
    //

    protected abstract void writeString(Diagnostic diagnostic);

    protected Consumer<Diagnostic> getDiagnosticCollector() { return diagnosticCollector; }

    protected Consumer<Diagnostic> getProblemCollector() { return problemCollector; }

    protected Consumer<String> getStringWriter() { return stringWriter; }

    protected boolean disableSystemOutput() { return disableSystemOutput; }

    protected boolean disableFlush() { return disableFlush; }

    protected boolean printStackTrace() { return printStackTrace; }

    protected void report(Diagnostic diagnostic) {
        if (this.level.compareTo(diagnostic.getLevel()) >= 0) {
            writeString(diagnostic);
            if (getDiagnosticCollector() != null) {
                getDiagnosticCollector().accept(diagnostic);
            }
        }

        if (getProblemCollector() != null && isProblem(level)) {
            getProblemCollector().accept(diagnostic);
        }
    }

    protected void writeString(Throwable cause, Level level, String message) {
        outputToConsole(cause, level, message);
        if (getStringWriter() != null) {
            getStringWriter().accept(message);
        }
    }

    protected void outputToConsole(Throwable cause, Level level, String message) {
        if (!disableSystemOutput()) {
            PrintStream console = level == Level.ERROR ? System.err : System.out;
            console.print(message);
            if (cause != null && printStackTrace()) {
                cause.printStackTrace();
            }
            if (!disableFlush()) {
                console.flush();
            }
        }
    }

    //
    //
    //

    /// With message string
    protected Diagnostic buildDiagnostic(String source, Level level, String message, Object... details) {
        return new Diagnostic(
            null,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, level, null, null, message, details
        );
    }

    /// With diagnostic code, and cause
    protected Diagnostic buildDiagnostic(String source, Level level, Throwable cause, DiagnosticCode code, Object... details) {
        return new Diagnostic(
            null,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, level, cause, code, null, details
        );
    }

    /// With diagnostic code, location, and cause
    protected Diagnostic buildDiagnostic(URI uri, int line, int column, int startOffset, int endOffset, String source, Level level, Throwable cause, DiagnosticCode code, Object... details) {
        return new Diagnostic(uri, line, column, startOffset, endOffset, source, level, cause, code, null, details);
    }

    /// With diagnostic code, token, and cause
    protected Diagnostic buildDiagnostic(Token token, String source, Level level, Throwable cause, DiagnosticCode code, Object... details) {
        if (token == null) {
            throw new IllegalArgumentException("Token must not be null");
        }
        return new Diagnostic(null, token, source, level, cause, code, null, details);
    }

    private boolean isProblem(Level level) {
        return (level == Level.ERROR || level == Level.WARN || level == Level.INFO);
    }
}
