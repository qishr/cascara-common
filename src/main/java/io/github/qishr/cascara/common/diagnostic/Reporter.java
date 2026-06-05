package io.github.qishr.cascara.common.diagnostic;

import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.token.Token;

public interface Reporter {

    /// Sets the level of output when logging directly to the console.
    Reporter setLevel(Level level);

    /// Registers a collector to receive all diagnostics processed by this reporter.
    /// This includes debugging info, trace states, warnings, and error diagnostics.
    ///
    /// @param collector The consumer that processes each produced [Diagnostic].
    Reporter setDiagnosticCollector(Consumer<Diagnostic> collector);

    /// Registers a specialized collector to receive only problem-level diagnostics.
    /// This collector is filtered to intercept only `Level.WARN` and `Level.ERROR` items.
    ///
    /// @param collector The consumer that processes problem [Diagnostic] objects.
    Reporter setProblemCollector(Consumer<Diagnostic> collector);

    /// Checks whether any active listener or collector is tracking problems.
    ///
    /// This can be used as an optimization flag by sub-parsers or AST-walkers
    /// to skip expensive location token captures or contextual allocations if nobody
    /// is actively listening for error diagnostics.
    ///
    /// @return `true` if warnings or errors are being collected, otherwise `false`.
    boolean collectsProblems();

    /// Reports a trace message through the reporter.
    /// @param format The format of the message to report.
    /// @param args Arguments referenced by the format specifiers in the format string.
    /// If the last argument is a Throwable, it will not be used in message formatting
    /// and will instead become a field in the Diagnostic produced by this call.
    void trace(String format, Object... args);

    /// Reports a debug message through the reporter.
    /// @param format The format of the message to report.
    /// @param args Arguments referenced by the format specifiers in the format string.
    /// If the last argument is a Throwable, it will not be used in message formatting
    /// and will instead become a field in the Diagnostic produced by this call.
    void debug(String format, Object... args);

    /// Reports an informational message through the reporter.
    /// @param format The format of the message to report.
    /// @param args Arguments referenced by the format specifiers in the format string.
    /// If the last argument is a Throwable, it will not be used in message formatting
    /// and will instead become a field in the Diagnostic produced by this call.
    void info(String format, Object... args);

    /// Reports a warning message including location information.
    /// @param code The code of this warning.
    /// @param format The format of the message to report.
    /// @param args Arguments referenced by the format specifiers in the format string.
    /// If the last argument is a Throwable, it will not be used in message formatting
    /// and will instead become a field in the Diagnostic produced by this call.
    void warn(String code, String format, Object... args);

    /// Reports an error message including location information.
    /// @param code The code of this error.
    /// @param format The format of the message to report.
    /// @param args Arguments referenced by the format specifiers in the format string.
    /// If the last argument is a Throwable, it will not be used in message formatting
    /// and will instead become a field in the Diagnostic produced by this call.
    void error(String code, String format, Object... args);

    /// Reports an informational message anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void infoAt(int line, int column, String format, Object... args);

    /// Reports a warning anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param code The semantic classification code for this warning.
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void warnAt(int line, int column, String code, String format, Object... args);

    /// Reports an error anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param code The semantic classification code for this error.
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void errorAt(int line, int column, String code, String format, Object... args);

    /// Reports an informational message anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void infoAt(int line, int column, int start, int end, String format, Object... args);

    /// Reports a warning anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param code The semantic classification code for this warning.
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void warnAt(int line, int column, int start, int end, String code, String format, Object... args);

    /// Reports an error anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param code The semantic classification code for this error.
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void errorAt(int line, int column, int start, int end, String code, String format, Object... args);

    /// Reports an informational message derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void infoAt(Token token, String format, Object... args);

    /// Reports a warning derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param code The semantic classification code for this warning.
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void warnAt(Token token, String code, String format, Object... args);

    /// Reports an error derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param code The semantic classification code for this error.
    /// @param format The format string.
    /// @param args Arguments for the format string. If the last argument is a [Throwable],
    ///             it maps directly to the diagnostic's cause.
    void errorAt(Token token, String code, String format, Object... args);
}