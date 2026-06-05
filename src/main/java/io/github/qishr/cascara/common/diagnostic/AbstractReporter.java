package io.github.qishr.cascara.common.diagnostic;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.exception.LocatableException;
import io.github.qishr.cascara.common.lang.token.Token;

public abstract class AbstractReporter<T extends AbstractReporter<?>> implements Reporter {
    protected Level level = Level.INFO;

    protected String source;

    /// Consumes diagnostics included in the current Level or more
    /// important, with ERROR being the most important.
    protected Consumer<Diagnostic> diagnosticCollector;

    // Consumes ERROR, WARN, and INFO diagnostics.
    protected Consumer<Diagnostic> problemCollector;

    protected Consumer<String> stringWriter;
    protected boolean disableSystemOutput = false;
    protected boolean disableFlush = true;

    protected AbstractReporter(Consumer<String> writer) {
        this.stringWriter = writer;
    }

    protected AbstractReporter() {
        // Nothing to see here
    }

    protected abstract T self();

    @Override
    public boolean collectsProblems() {
        return problemCollector != null;
    }

    @Override
    public T setLevel(Level level) {
        this.level = level;
        return self();
    }

    @Override
    public T setDiagnosticCollector(Consumer<Diagnostic> collector) {
        diagnosticCollector = collector;
        return self();
    }

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

    //
    // Plain
    //

    @Override
    public void trace(String format, Object... args) {
        report(buildDiagnostic(source, Level.TRACE, null, format, args));
    }

    @Override
    public void debug(String format, Object... args) {
        report(buildDiagnostic(source, Level.DEBUG, null, format, args));
    }

    @Override
    public void info(String format, Object... args) {
        report(buildDiagnostic(source, Level.INFO, null, format, args));
    }

    @Override
    public void warn(String code, String format, Object... args) {
        report(buildDiagnostic(source, Level.WARN, code, format, args));
    }

    @Override
    public void error(String code, String format, Object... args) {
        report(buildDiagnostic(source, Level.ERROR, code, format, args));
    }

    //
    // With Location
    //

    @Override
    public void infoAt(int line, int column, String format, Object... args) {
        report(buildDiagnostic(
            line, column,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, Level.INFO, null,
            format, args
        ));
    }

    @Override
    public void warnAt(int line, int column, String code, String format, Object... args) {
        report(buildDiagnostic(
            line, column,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, Level.WARN, code,
            format, args
        ));
    }

    @Override
    public void errorAt(int line, int column, String code, String format, Object... args) {
        report(buildDiagnostic(
            line, column,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, Level.ERROR, code,
            format, args
        ));
    }

    //
    // With Location Including Offset
    //

    @Override
    public void infoAt(int line, int column, int start, int end, String format, Object... args) {
        report(buildDiagnostic(line, column, start, end, source, Level.INFO, null, format, args));
    }

    @Override
    public void warnAt(int line, int column, int start, int end, String code, String format, Object... args) {
        report(buildDiagnostic(line, column, start, end, source, Level.WARN, code, format, args));
    }

    @Override
    public void errorAt(int line, int column, int start, int end, String code, String format, Object... args) {
        report(buildDiagnostic(line, column, start, end, source, Level.ERROR, code, format, args));
    }

    //
    // With Token
    //

    @Override
    public void infoAt(Token token, String format, Object... args) {
        report(buildDiagnostic(token, source, Level.INFO, null, format, args));
    }

    @Override
    public void warnAt(Token token, String code, String format, Object... args) {
        report(buildDiagnostic(token, source, Level.WARN, code, format, args));
    }

    @Override
    public void errorAt(Token token, String code, String format, Object... args) {
        report(buildDiagnostic(token, source, Level.ERROR, code, format, args));
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

    protected void writeString(Level level, String message) {
        outputToConsole(level, message);
        if (getStringWriter() != null) {
            getStringWriter().accept(message);
        }
    }

    protected void outputToConsole(Level level, String message) {
        PrintStream console = level == Level.ERROR ? System.err : System.out;
        if (!disableSystemOutput()) {
            console.print(message);
            if (!disableFlush()) {
                console.flush();
            }
        }
    }

    //
    //
    //

    private Diagnostic buildDiagnostic(String source, Level level, String code, String format, Object... args) {
        return buildDiagnostic(
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            source, level, code,
            format, args
        );
    }

    private Diagnostic buildDiagnostic(Token token, String source, Level level, String code, String format, Object... args) {
        return buildDiagnostic(
            token.getStartLine(),
            token.getStartColumn(),
            token.getOffset(),
            token.getOffset() + token.getLexeme().length(),
            source, level, code, format, args);
    }

    private Diagnostic buildDiagnostic(int line, int column, int start, int end, String source, Level level, String code, String format, Object... args) {
        if (format == null || format.isBlank()) {
            return new Diagnostic(
                null, line, column, start, end, source, level, code, "", null
            );
        } else {
            if (args.length == 0) {
                return new Diagnostic(null, line, column, start, end, source, level, code, format, null);
            } else {
                Object last = args[args.length - 1];
                if (last instanceof Throwable cause) {
                    String message = String.format(format, Arrays.<Object>copyOfRange(args, 0, args.length - 1));
                    return new Diagnostic(null, line, column, start, end, source, level, code, message, cause);
                } else {
                    String message = String.format(format, args);
                    return new Diagnostic(null, line, column, start, end, source, level, code, message, null);
                }
            }
        }
    }

    private boolean isProblem(Level level) {
        return (level == Level.ERROR || level == Level.WARN || level == Level.INFO);
    }
}
