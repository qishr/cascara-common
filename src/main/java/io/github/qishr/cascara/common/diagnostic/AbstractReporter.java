package io.github.qishr.cascara.common.diagnostic;

import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.exception.LocatableException;
import io.github.qishr.cascara.common.lang.token.Token;

public abstract class AbstractReporter<T extends AbstractReporter<?>> implements Reporter {
    protected Level level = Level.INFO;

    protected Class<?> clazz;

    // Consumes diagnostics included in the current Level or more
    // important, with ERROR being the most important.
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
        report(buildDiagnostic(clazz, Level.TRACE, null, format, args));
    }

    @Override
    public void debug(String format, Object... args) {
        report(buildDiagnostic(clazz, Level.DEBUG, null, format, args));
    }

    @Override
    public void info(String format, Object... args) {
        report(buildDiagnostic(clazz, Level.INFO, null, format, args));
    }

    @Override
    public void warn(String format, Object... args) {
        report(buildDiagnostic(clazz, Level.WARN, null, format, args));
    }

    @Override
    public void error(String format, Object... args) {
        report(buildDiagnostic(clazz, Level.ERROR, null, format, args));
    }

    //
    // With Location
    //

    @Override
    public void infoAt(int start, int end, int line, int column, URI uri, String format, Object... args) {
        report(buildDiagnostic(clazz, Level.INFO, start, end, line, column, uri, format, args));
    }

    @Override
    public void warnAt(int start, int end, int line, int column, URI uri, String format, Object... args) {
        report(buildDiagnostic(clazz, Level.WARN, start, end, line, column, uri, format, args));
    }

    @Override
    public void errorAt(int start, int end, int line, int column, URI uri, String format, Object... args) {
        report(buildDiagnostic(clazz, Level.ERROR, start, end, line, column, uri, format, args));
    }

    //
    // With Token
    //

    @Override
    public void infoAt(Token token, URI uri, String format, Object... args) {
        report(buildDiagnostic(clazz, Level.INFO, token, uri, format, args));
    }

    @Override
    public void warnAt(Token token, URI uri, String format, Object... args) {
        report(buildDiagnostic(clazz, Level.WARN, token, uri, format, args));
    }

    @Override
    public void errorAt(Token token, URI uri, String format, Object... args) {
        report(buildDiagnostic(clazz, Level.ERROR, token, uri, format, args));
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

    private Diagnostic buildDiagnostic(Class<?> clazz, Level level, URI uri, String format, Object... args) {
        return buildDiagnostic(
            clazz, level,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            LocatableException.UNKNOWN_COORD,
            uri, format, args
        );
    }

    private Diagnostic buildDiagnostic(Class<?> clazz, Level level, Token token, URI uri, String format, Object... args) {
        return buildDiagnostic(clazz, level, token.getOffset(), token.getOffset() + token.getLexeme().length(), token.getStartLine(), token.getStartColumn(), uri, format, args);
    }

    private Diagnostic buildDiagnostic(Class<?> clazz, Level level, int start, int end, int line, int column, URI uri, String format, Object... args) {
        if (format == null || format.isBlank()) {
            return new Diagnostic(clazz, level, "", start, end, line, column, uri, null);
        } else {
            if (args.length == 0) {
                return new Diagnostic(clazz, level, format, start, end, line, column, uri, null);
            } else {
                Object last = args[args.length - 1];
                if (last instanceof Throwable cause) {
                    String message = String.format(format, Arrays.<Object>copyOfRange(args, 0, args.length - 1));
                    return new Diagnostic(clazz, level, message, start, end, line, column, uri, cause);
                } else {
                    String message = String.format(format, args);
                    return new Diagnostic(clazz, level, message, start, end, line, column, uri, null);
                }
            }
        }
    }

    private boolean isProblem(Level level) {
        return (level == Level.ERROR || level == Level.WARN || level == Level.INFO);
    }
}
