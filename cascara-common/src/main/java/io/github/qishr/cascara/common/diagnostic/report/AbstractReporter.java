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

package io.github.qishr.cascara.common.diagnostic.report;

import java.net.URI;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.DiagnosticBuilder;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableException;
import io.github.qishr.cascara.common.diagnostic.exception.LocatableException;
import io.github.qishr.cascara.common.diagnostic.format.ColoredLogFormatter;
import io.github.qishr.cascara.common.diagnostic.format.PlainLogFormatter;
import io.github.qishr.cascara.common.diagnostic.log.ConsoleLogger;
import io.github.qishr.cascara.common.diagnostic.log.LineLogger;
import io.github.qishr.cascara.common.diagnostic.log.Logger;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.lang.token.Token;

public abstract class AbstractReporter<T extends AbstractReporter<?>> implements Reporter {

    protected static final ZoneId UTC = ZoneId.of("UTC");



    protected Level level = Level.INFO;

    /// The simple name of the class that made the report
    protected String source;

    protected Serializer<?> serializer;

    // TODO:
    protected List<Logger> loggers = new ArrayList<>();
    protected ConsoleLogger consoleLogger; // = new ConsoleLogger();
    protected LineLogger lineLogger; // = new LineLogger();

    /// Consumes diagnostics included in the current Level or more
    /// important, with ERROR being the most important.
    protected Consumer<Diagnostic> diagnosticConsumer;

    /// Consumes ERROR, WARN, and INFO diagnostics.
    protected Consumer<Diagnostic> problemConsumer;

    // /// Consumes every line of diagnostic output as a String.
    // protected Consumer<String> lineConsumer;

    protected boolean flushEnabled = false;
    protected boolean systemOutputEnabled = true;
    protected boolean systemErrorEnabled = false;
    protected boolean stackTraceEnabled = false;
    protected boolean showProblemCodes = false;
    protected boolean prefixEveryLine = true;
    protected boolean ansiColoringEnabled;

    protected ReportWriter[] writers = new ReportWriter[7];

    protected AbstractReporter() {
        writers[Level.ERROR.ordinal()] = new ReportWriter(this, Level.ERROR);
        writers[Level.WARN.ordinal()] = new ReportWriter(this, Level.WARN);
        writers[Level.INFO.ordinal()] = new ReportWriter(this, Level.INFO);
        writers[Level.DEBUG.ordinal()] = new ReportWriter(this, Level.DEBUG);
        writers[Level.TRACE.ordinal()] = new ReportWriter(this, Level.TRACE);
    }

    protected abstract T self();

    public T addLogger(Logger logger) {
        loggers.add(logger);
        return self();
    }

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
    public T setLineConsumer(Consumer<String> consumer) {
        lineLogger.setLineConsumer(consumer);
        return self();
    }

    /// {@inheritDoc}
    @Override
    public T setDiagnosticConsumer(Consumer<Diagnostic> consumer) {
        diagnosticConsumer = consumer;
        return self();
    }

    /// {@inheritDoc}
    @Override
    public T setProblemConsumer(Consumer<Diagnostic> consumer) {
        problemConsumer = consumer;
        return self();
    }

    public T setSystemOutputEnabled(boolean b) {
        // TODO: Make this controllable via system properties, at least for GlobalReporter
        systemOutputEnabled = b;
        return self();
    }

    public T setFlushEnabled(boolean b) {
        flushEnabled = b;
        return self();
    }

    public T setStackTraceEnabled(boolean b) {
        // TODO: Make this controllable via system properties, at least for GlobalReporter
        stackTraceEnabled = b;
        return self();
    }

    public T setSystemErrorEnabled(boolean b) {
        // TODO: Make this controllable via system properties, at least for GlobalReporter
        systemErrorEnabled = b;
        return self();
    }

    public T setAnsiColoringEnabled(boolean b) {
        // TODO: Allow this to be disabled via env var and/or system proeprty
        if (b) {
            consoleLogger.setFormatter(new ColoredLogFormatter());
        } else {
            consoleLogger.setFormatter(new PlainLogFormatter());
        }
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

    public boolean reportsDebug() {
        return !isSilent() && level.includes(Level.DEBUG);
    }

    public boolean reportsTrace() {
        return !isSilent() && level.includes(Level.TRACE);
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
                report(DiagnosticBuilder.build(
                    locatable.getUri(),
                    locatable.getLine(),
                    locatable.getColumn(),
                    Diagnostic.UNKNOWN_COORD,
                    Diagnostic.UNKNOWN_COORD,
                    source, Level.ERROR,
                    getStackTrace(),
                    localizable.getCause(),
                    localizable.getDiagnosticMessage(), localizable.getDetails()));
            } else {
                report(DiagnosticBuilder.build(
                    source, Level.ERROR,
                    getStackTrace(),
                    localizable.getCause(),
                    localizable.getDiagnosticMessage(), localizable.getDetails())
                );
            }
        } else {
            report(DiagnosticBuilder.build(
                source, Level.ERROR,
                getStackTrace(),
                e.getCause(),
                GenericMessage.EXCEPTION, e.getMessage())
            );
        }
    }

    //
    // Plain
    //

    /// {@inheritDoc}
    @Override
    public void trace(String message, Object... details) {
        report(DiagnosticBuilder.build(source, Level.TRACE, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void debug(String message, Object... details) {
        report(DiagnosticBuilder.build(source, Level.DEBUG, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void info(DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(source, Level.INFO, null, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void warn(DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(source, Level.WARN, getStackTrace(), null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void error(DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(source, Level.ERROR, getStackTrace(), null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void error(Throwable cause, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(source, Level.ERROR, getStackTrace(), cause, code, details));
    }

    //
    // With Location
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(int line, int column, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.INFO, null, null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(int line, int column, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, getStackTrace(), null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, Throwable cause, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), cause, code, details
        ));
    }

    //
    // With Location Including Offset
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(int line, int column, int startOffset, int endOffset, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(null, line, column, startOffset, endOffset, source, Level.INFO, null, null, code, null, details));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(int line, int column, int startOffset, int endOffset, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(null, line, column, startOffset, endOffset, source, Level.WARN, getStackTrace(), null, code, null, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, int startOffset, int endOffset, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(null, line, column, startOffset, endOffset, source, Level.ERROR, getStackTrace(), null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, int startOffset, int endOffset, Throwable cause, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(null, line, column, startOffset, endOffset, source, Level.ERROR, getStackTrace(), cause, code, details));
    }

    //
    // With Token
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(Token token, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(token, source, Level.INFO, null, null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(Token token, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(token, source, Level.WARN, getStackTrace(), null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(Token token, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(token, source, Level.ERROR, getStackTrace(), null, code, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(Token token, Throwable cause, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(token, source, Level.ERROR, getStackTrace(), cause, code, details));
    }

    //
    // With URI
    //

    /// {@inheritDoc}
    @Override
    public void warnAt(URI uri, int line, int column, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            uri, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, getStackTrace(), null, code, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(URI uri, int line, int column, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            uri, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), null, code, details
        ));
    }

    @Override
    public void warnAt(URI uri, Token token, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, getStackTrace(), null, code, details
        ));
    }

    @Override
    public void errorAt(URI uri, Token token, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), null, code, details
        ));
    }

    @Override
    public void errorAt(URI uri, Token token, Throwable cause, DiagnosticMessage code, Object... details) {
        report(DiagnosticBuilder.build(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), cause, code, details
        ));
    }

    //
    //
    //

    // protected abstract String formatMessage(Diagnostic diagnostic, String line, int lineNumber, boolean ansiColoring);

    protected Consumer<Diagnostic> getDiagnosticConsumer() { return diagnosticConsumer; }

    protected Consumer<Diagnostic> getProblemConsumer() { return problemConsumer; }

    // protected Consumer<String> getLineConsumer() { return lineConsumer; }

    protected boolean isSystemOutputEnabled() { return systemOutputEnabled; }

    protected boolean isFlushEnabled() { return flushEnabled; }

    protected boolean isStackTraceEnabled() { return stackTraceEnabled; }

    protected boolean isProblem(Level level) {
        return (level == Level.ERROR || level == Level.WARN || level == Level.INFO);
    }

    //
    //
    //


    /// Central reporting method. All other rporting methods call this.
    protected void report(Diagnostic diagnostic) {
        if (level.includes(diagnostic.getLevel())) {
            reportAnyLevel(diagnostic);
        }
    }

    protected void reportAnyLevel(Diagnostic diagnostic) {
        log(diagnostic);

        // if (getLineConsumer() != null) {
        //     String logLine = "[" + level.getLogPrefix() + "] " + msgLine;
        //     getLineConsumer().accept(logLine);
        // }
        if (getDiagnosticConsumer() != null) {
            getDiagnosticConsumer().accept(diagnostic);
        }
        if (getProblemConsumer() != null && isProblem(level)) {
            getProblemConsumer().accept(diagnostic);
        }
    }

    protected void log(Diagnostic diagnostic) {
        if (isSystemOutputEnabled()) {
            consoleLogger.log(diagnostic);
        }

        // TODO: All loggers

        for (Logger logger : loggers) {
            logger.log(diagnostic);
        }

        if (lineLogger != null) {
            lineLogger.log(diagnostic);
        }
    }

    protected void logLine(Level level, String msgLine) {
        Diagnostic diagnostic = DiagnosticBuilder.build("", level, msgLine);
        log(diagnostic);
    }

    private StackTraceElement[] getStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return Arrays.copyOfRange(stackTrace, 3, stackTrace.length);
    }
}
