// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.


package io.github.qishr.cascara.common.diagnostic;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;
import io.github.qishr.cascara.common.lang.token.Token;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.TermUtils;

public abstract class AbstractReporter<T extends AbstractReporter<?>> implements Reporter {
    protected static final boolean CAN_USE_ANSI_COLORING = (
        JreUtils.isRunningInTerminal() ||
        JreUtils.isRunningViaEclipse() ||
        JreUtils.isRunningViaGradle()
    );

    protected static final String[] levelColors = new String[7];
    {
        // levelColors[Level.DEFAULT.ordinal()] = ANSI_WHITE;
        levelColors[Level.ERROR.ordinal()] = TermUtils.ANSI_RED;
        levelColors[Level.WARN.ordinal()] = TermUtils.ANSI_YELLOW;
        levelColors[Level.INFO.ordinal()] = TermUtils.ANSI_BLUE;
        // levelColors[Level.DEBUG.ordinal()] = TermUtils.ANSI_WHITE;
        // levelColors[Level.TRACE.ordinal()] = TermUtils.ANSI_WHITE;
    }

    protected boolean ansiColoringEnabled;

    protected Level level = Level.INFO;

    /// The simple name of the class that made the report
    protected String source;

    /// Consumes diagnostics included in the current Level or more
    /// important, with ERROR being the most important.
    protected Consumer<Diagnostic> diagnosticConsumer;

    /// Consumes ERROR, WARN, and INFO diagnostics.
    protected Consumer<Diagnostic> problemConsumer;

    /// Consumes every line of diagnostic output as a String.
    protected Consumer<String> lineConsumer;

    protected boolean flushEnabled = false;
    protected boolean systemOutputEnabled = true;
    protected boolean systemErrorEnabled = false;
    protected boolean stackTraceEnabled = false;
    protected boolean showProblemCodes = false;
    protected boolean prefixEveryLine = true;

    protected ReportWriter[] writers = new ReportWriter[7];

    protected AbstractReporter() {
        this(null);
    }

    protected AbstractReporter(Consumer<String> logger) {
        this.lineConsumer = logger;
        writers[Level.ERROR.ordinal()] = new ReportWriter(this, Level.ERROR);
        writers[Level.WARN.ordinal()] = new ReportWriter(this, Level.WARN);
        writers[Level.INFO.ordinal()] = new ReportWriter(this, Level.INFO);
        writers[Level.DEBUG.ordinal()] = new ReportWriter(this, Level.DEBUG);
        writers[Level.TRACE.ordinal()] = new ReportWriter(this, Level.TRACE);
        setAnsiColoringEnabled(true);
    }

    protected abstract T self();

    /// {@inheritDoc}
    @Override
    public boolean collectsProblems() {
        return problemConsumer != null;
    }

    /// {@inheritDoc}
    @Override
    public T setLevel(Level level) {
        this.level = level;
        return self();
    }

    /// {@inheritDoc}
    @Override
    public T setLineConsumer(Consumer<String> logger) {
        this.lineConsumer = logger;
        return self();
    }

    /// {@inheritDoc}
    @Override
    public T setDiagnosticConsumer(Consumer<Diagnostic> collector) {
        diagnosticConsumer = collector;
        return self();
    }

    /// {@inheritDoc}
    @Override
    public T setProblemConsumer(Consumer<Diagnostic> collector) {
        problemConsumer = collector;
        return self();
    }

    public T setSystemOutputEnabled(boolean b) {
        systemOutputEnabled = b;
        return self();
    }

    public T setFlushEnabled(boolean b) {
        flushEnabled = b;
        return self();
    }

    public T setStackTraceEnabled(boolean b) {
        stackTraceEnabled = b;
        return self();
    }

    public T setSystemErrorEnabled(boolean b) {
        systemErrorEnabled = b;
        return self();
    }

    public T setAnsiColoringEnabled(boolean b) {
        ansiColoringEnabled = CAN_USE_ANSI_COLORING && b;
        return self();
    }

    @Experimental
    public T setPrefixEveryLine(boolean b) {
        prefixEveryLine = b;
        return self();
    }

    public T setShowProblemCodes(boolean b) {
        showProblemCodes = b;
        return self();
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public boolean isSilent() {
        return false;
    }

    public ReportWriter getWriter(Diagnostic.Level level) {
        return writers[level.ordinal()];
    }

    //
    // Exception
    //

    /// {@inheritDoc}
    @Override
    public void error(Exception e) {
        if (e instanceof LocalizableException localizable) {
            if (e instanceof LocatableException locatable) {
                report(buildDiagnostic(
                    locatable.getUri(),
                    locatable.getLine(),
                    locatable.getColumn(),
                    Diagnostic.UNKNOWN_COORD,
                    Diagnostic.UNKNOWN_COORD,
                    source, Level.ERROR, localizable.getCause(),
                    localizable.getCode(), localizable.getDetails()));
            } else {
                report(buildDiagnostic(
                    source, Level.ERROR, localizable.getCause(),
                    localizable.getCode(), localizable.getDetails())
                );
            }
        } else {
            report(buildDiagnostic(
                source, Level.ERROR, e.getCause(),
                GenericDiagnosticCode.EXCEPTION, e.getMessage())
            );
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
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.INFO, null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(int line, int column, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, Throwable cause, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
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
    // With URI
    //

    /// {@inheritDoc}
    @Override
    public void warnAt(URI uri, int line, int column, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            uri, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(URI uri, int line, int column, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            uri, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, null, code, details
        ));
    }

    @Override
    public void warnAt(URI uri, Token token, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, null, code, details
        ));
    }

    @Override
    public void errorAt(URI uri, Token token, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, null, code, details
        ));
    }

    @Override
    public void errorAt(URI uri, Token token, Throwable t, DiagnosticCode code, Object... details) {
        report(buildDiagnostic(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, t, code, details
        ));
    }

    //
    //
    //

    protected abstract String formatMessage(Diagnostic diagnostic, String line, int lineNumber, boolean ansiColoring);

    protected Consumer<Diagnostic> getDiagnosticConsumer() { return diagnosticConsumer; }

    protected Consumer<Diagnostic> getProblemConsumer() { return problemConsumer; }

    protected Consumer<String> getLineConsumer() { return lineConsumer; }

    protected boolean isSystemOutputEnabled() { return systemOutputEnabled; }

    protected boolean isFlushEnabled() { return flushEnabled; }

    protected boolean isStackTraceEnabled() { return stackTraceEnabled; }

    protected void report(Diagnostic diagnostic) {
        if (this.level.compareTo(diagnostic.getLevel()) >= 0) {
            writeString(diagnostic);
            if (getDiagnosticConsumer() != null) {
                getDiagnosticConsumer().accept(diagnostic);
            }
        }

        if (getProblemConsumer() != null && isProblem(level)) {
            getProblemConsumer().accept(diagnostic);
        }
    }

    protected void writeString(Diagnostic diagnostic) {
        ReportWriter writer = writers[diagnostic.getLevel().ordinal()];
        if (writer == null) {
            return;
        }
        String[] lines = diagnostic.getMessage().split("\n");
        for (int i = 0; i < lines.length; i++) {
            String logLine = formatMessage(diagnostic, lines[i], i, false).stripTrailing();
            String consoleLine = ansiColoringEnabled
                ? formatMessage(diagnostic, lines[i], i, true).stripTrailing()
                : logLine;
            writer.logLine(logLine, i);
            writer.outputLine(consoleLine, i);
        }
        if (diagnostic.getCause() != null && isStackTraceEnabled()) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            diagnostic.getCause().printStackTrace(pw);
            lines = sw.toString().split("\n");
            for (int i = 0; i < lines.length; i++) {
                String logLine = lines[i];
                writer.logLine(logLine, i);
                writer.outputLine(logLine, i);
            }
        }
    }

    protected void logLine(Level level, String msgLine, int msgLineNumber) {
        if (getLineConsumer() != null) {
            String logLine = "[" + level.getLogPrefix() + "] " + msgLine;
            getLineConsumer().accept(logLine);
        }
    }

    protected void displayLine(Level diagnosticLevel, String msgLine, int msgLineNumber) {
        if (isSystemOutputEnabled()) {
            boolean indented = msgLine.startsWith(" ");
            PrintStream stream = (diagnosticLevel == Level.ERROR && systemErrorEnabled) ? System.err : System.out;

            if (prefixEveryLine || msgLineNumber == 0) {
                stream.print("[");
                if (ansiColoringEnabled) {
                    String ansiCode = levelColors[diagnosticLevel.ordinal()];
                    if (ansiCode != null) {
                        stream.print(ansiCode);
                        stream.print(diagnosticLevel.getLogPrefix());
                        stream.print(TermUtils.ANSI_RESET);
                    } else {
                        stream.print(diagnosticLevel.getLogPrefix());
                    }
                } else {
                    stream.print(diagnosticLevel.getLogPrefix());
                }
                stream.print("] ");
            } else {
                stream.print("        ");
            }

            if (indented && ansiColoringEnabled) {
                stream.print(TermUtils.ANSI_GREEN);
                stream.print(msgLine);
                stream.print(TermUtils.ANSI_RESET);
            } else {
                stream.print(msgLine);
            }
            stream.print("\n");
            if (isFlushEnabled()) {
                stream.flush();
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
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, level, null, null, message, details
        );
    }

    /// With diagnostic code, and cause
    protected Diagnostic buildDiagnostic(String source, Level level, Throwable cause, DiagnosticCode code, Object... details) {
        return new Diagnostic(
            null,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
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

    protected boolean isProblem(Level level) {
        return (level == Level.ERROR || level == Level.WARN || level == Level.INFO);
    }

    public boolean reportsDebug() {
        return !isSilent() && level.includes(Level.DEBUG);
    }

    public boolean reportsTrace() {
        return !isSilent() && level.includes(Level.TRACE);
    }
}
