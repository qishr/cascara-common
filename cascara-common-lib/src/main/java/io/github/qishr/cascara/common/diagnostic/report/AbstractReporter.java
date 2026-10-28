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
import io.github.qishr.cascara.common.diagnostic.format.FormatOptions;
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

    /// The simple name of the class that made the report
    protected String source;

    protected Serializer<?> serializer;

    protected FormatOptions formatOptions = defaultFormatOptions();
    protected ReportWriter[] writers = new ReportWriter[7];
    protected List<Logger> loggers = new ArrayList<>();
    protected ConsoleLogger consoleLogger;
    protected LineLogger lineLogger;

    /// Consumes diagnostics included in the current Level or more
    /// important, with ERROR being the most important.
    protected Consumer<Diagnostic> diagnosticConsumer;

    /// Consumes ERROR, WARN, and INFO diagnostics.
    protected Consumer<Diagnostic> problemConsumer;

    protected Level level = Level.INFO;

    // ConsoleLogger options
    protected boolean systemOutputEnabled = true;
    protected boolean flushEnabled = false;
    protected boolean systemErrorEnabled = false;
    protected boolean ansiColoringEnabled;


    // TODO: Lazy getters for these...
    protected AbstractReporter() {
        writers[Level.ERROR.ordinal()] = new ReportWriter(this, Level.ERROR);
        writers[Level.WARN.ordinal()] = new ReportWriter(this, Level.WARN);
        writers[Level.INFO.ordinal()] = new ReportWriter(this, Level.INFO);
        writers[Level.DEBUG.ordinal()] = new ReportWriter(this, Level.DEBUG);
        writers[Level.TRACE.ordinal()] = new ReportWriter(this, Level.TRACE);
    }

    protected abstract T self();

    /// To be overridden by reporters neding specific default options
    protected FormatOptions defaultFormatOptions() {
        return new FormatOptions();
    }

    public T addLogger(Logger logger) {
        loggers.add(logger);
        return self();
    }

    // TODO: removeLogger

    @Override
    public Level getLevel() {
        return level;
    }

    /// {@inheritDoc}
    @Override
    public T setLevel(Level level) {
        this.level = level;
        return self();
    }




    //================================
    // TODO: These should perhaps be able to add/remove consumers (trackers?)

    /// {@inheritDoc}
    @Experimental
    @Override
    public T setLineConsumer(Consumer<String> consumer) {
        lineLogger.setLineConsumer(consumer);
        return self();
    }

    @Experimental
    protected Consumer<Diagnostic> getDiagnosticConsumer() {
        return diagnosticConsumer;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public T setDiagnosticConsumer(Consumer<Diagnostic> consumer) {
        diagnosticConsumer = consumer;
        return self();
    }

    @Experimental
    protected Consumer<Diagnostic> getProblemConsumer() {
        return problemConsumer;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public T setProblemConsumer(Consumer<Diagnostic> consumer) {
        problemConsumer = consumer;
        return self();
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public boolean collectsProblems() {
        return problemConsumer != null;
    }

    //================================






    public T setStackTraceEnabled(boolean b) {
        formatOptions.setIncludeStackTrace(b);
        // TODO: Make this controllable via system properties, at least for GlobalReporter
        return self();
    }

    @Experimental
    public T setPrefixEveryLine(boolean b) {
        formatOptions.setPrefixEveryLine(b);
        return self();
    }

    public T setShowProblemCodes(boolean b) {
        formatOptions.setIncludeProblemCodes(b);
        return self();
    }

    //
    // ConsoleLogger options
    //

    // TODO: Make these controllable via system properties, at least for GlobalReporter

    protected boolean isSystemOutputEnabled() { return systemOutputEnabled; }

    /// Controls whether diagnostic logging to console is enabled.
    public T setSystemOutputEnabled(boolean b) {
        systemOutputEnabled = b;
        return self();
    }

    protected boolean isImmediateFlushEnabled() { return flushEnabled; }

    /// Controls whether diagnostics logged to console are flushed immediately.
    public T setImmediateFlushEnabled(boolean b) {
        if (consoleLogger != null) {
            consoleLogger.setSystemErrorEnabled(b);
        }
        flushEnabled = b;
        return self();
    }

    /// Controls whether errors logged to console go to System.out or System.err.
    public T setSystemErrorEnabled(boolean b) {
        if (consoleLogger != null) {
            consoleLogger.setSystemErrorEnabled(b);
        }
        systemErrorEnabled = b;
        return self();
    }

    /// Controls whether diagnostics logged to console use ANSI coloring.
    public T setAnsiColoringEnabled(boolean b) {
        if (consoleLogger != null) {
            if (b) {
                consoleLogger.setFormatter(new ColoredLogFormatter(formatOptions));
            } else {
                consoleLogger.setFormatter(new PlainLogFormatter(formatOptions));
            }
        }
        return self();
    }

    //
    //
    //

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
    public void info(DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(source, Level.INFO, null, null, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void warn(DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(source, Level.WARN, getStackTrace(), null, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void error(DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(source, Level.ERROR, getStackTrace(), null, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void error(Throwable cause, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(source, Level.ERROR, getStackTrace(), cause, message, details));
    }

    //
    // With Location
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(int line, int column, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.INFO, null, null, message, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(int line, int column, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, getStackTrace(), null, message, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), null, message, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, Throwable cause, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            null, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), cause, message, details
        ));
    }

    //
    // With Location Including Offset
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(int line, int column, int startOffset, int endOffset, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(null, line, column, startOffset, endOffset, source, Level.INFO, null, null, message, null, details));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(int line, int column, int startOffset, int endOffset, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(null, line, column, startOffset, endOffset, source, Level.WARN, getStackTrace(), null, message, null, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, int startOffset, int endOffset, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(null, line, column, startOffset, endOffset, source, Level.ERROR, getStackTrace(), null, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(int line, int column, int startOffset, int endOffset, Throwable cause, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(null, line, column, startOffset, endOffset, source, Level.ERROR, getStackTrace(), cause, message, details));
    }

    //
    // With Token
    //

    /// {@inheritDoc}
    @Override
    public void infoAt(Token token, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(token, source, Level.INFO, null, null, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void warnAt(Token token, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(token, source, Level.WARN, getStackTrace(), null, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(Token token, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(token, source, Level.ERROR, getStackTrace(), null, message, details));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(Token token, Throwable cause, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(token, source, Level.ERROR, getStackTrace(), cause, message, details));
    }

    //
    // With URI
    //

    /// {@inheritDoc}
    @Override
    public void warnAt(URI uri, int line, int column, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            uri, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, getStackTrace(), null, message, details
        ));
    }

    /// {@inheritDoc}
    @Override
    public void errorAt(URI uri, int line, int column, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            uri, line, column,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), null, message, details
        ));
    }

    @Override
    public void warnAt(URI uri, Token token, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.WARN, getStackTrace(), null, message, details
        ));
    }

    @Override
    public void errorAt(URI uri, Token token, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), null, message, details
        ));
    }

    @Override
    public void errorAt(URI uri, Token token, Throwable cause, DiagnosticMessage message, Object... details) {
        report(DiagnosticBuilder.build(
            uri, token.getStartLine(), token.getStartColumn(),
            token.getOffset(),
            Diagnostic.UNKNOWN_COORD,
            source, Level.ERROR, getStackTrace(), cause, message, details
        ));
    }

    //
    // Internal logging and reporting methods
    //

    /// Central reporting method. All other rporting methods call this.
    protected void report(Diagnostic diagnostic) {
        if (level.includes(diagnostic.getLevel())) {
            reportAnyLevel(diagnostic);
        }
    }

    protected void reportAnyLevel(Diagnostic diagnostic) {
        log(diagnostic);
        if (getDiagnosticConsumer() != null) {
            getDiagnosticConsumer().accept(diagnostic);
        }
        if (getProblemConsumer() != null && isProblem(level)) {
            getProblemConsumer().accept(diagnostic);
        }
    }

    protected void log(Diagnostic diagnostic) {
        if (consoleLogger != null && isSystemOutputEnabled()) {
            consoleLogger.log(diagnostic);
        }
        if (lineLogger != null) {
            lineLogger.log(diagnostic);
        }
        for (Logger logger : loggers) {
            logger.log(diagnostic);
        }
    }

    /// Used by LogWriter to log lines of text
    protected void logLine(Level level, String msgLine) {
        // TODO source should be null, but test it
        Diagnostic diagnostic = DiagnosticBuilder.build("", level, msgLine);
        log(diagnostic);
    }

    protected boolean isProblem(Level level) {
        return (level == Level.ERROR || level == Level.WARN || level == Level.INFO);
    }

    /// Returns the stack trace of the calling site
    private StackTraceElement[] getStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return Arrays.copyOfRange(stackTrace, 3, stackTrace.length);
    }
}
