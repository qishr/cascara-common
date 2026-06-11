package io.github.qishr.cascara.common.diagnostic;

import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
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

    //
    // Reporting Methods
    //

    /// Reports a trace message through the reporter.
    /// @param format The format of the message to report.
    /// @param details Arguments referenced by the format specifiers in the format string.
    void trace(String format, Object... details);

    /// Reports a debug message through the reporter.
    /// @param format The format of the message to report.
    /// @param details Arguments referenced by the format specifiers in the format string.
    void debug(String format, Object... details);

    /// Reports an informational message through the reporter.
    /// @param code The code of this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void info(DiagnosticCode code, Object... details);

    /// Reports a warning message including location information.
    /// @param code The code of this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void warn(DiagnosticCode code, Object... details);

    /// Reports an error message including location information.
    /// @param code The code of this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void error(DiagnosticCode code, Object... details);

    /// Reports an error message including location information.
    /// @param cause The cause of this report.
    /// @param code The code of this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void error(Throwable cause, DiagnosticCode code, Object... details);

    /// Reports an informational message anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param code The semantic classification code for this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void infoAt(int line, int column, DiagnosticCode code, Object... details);

    /// Reports a warning anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param code The semantic classification code for this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void warnAt(int line, int column, DiagnosticCode code, Object... details);

    /// Reports an error anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(int line, int column, DiagnosticCode code, Object... details);

    /// Reports an error anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param cause The cause of this report.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(int line, int column, Throwable cause, DiagnosticCode code, Object... details);

    /// Reports an informational message anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void infoAt(int line, int column, int start, int end, DiagnosticCode code, Object... details);

    /// Reports a warning anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param code The semantic classification code for this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void warnAt(int line, int column, int start, int end, DiagnosticCode code, Object... details);

    /// Reports an error anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(int line, int column, int start, int end, DiagnosticCode code, Object... details);

    /// Reports an error anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param cause The cause of this report.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(int line, int column, int start, int end, Throwable cause, DiagnosticCode code, Object... details);

    /// Reports an informational message derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void infoAt(Token token, DiagnosticCode code, Object... details);

    /// Reports a warning derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param code The semantic classification code for this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void warnAt(Token token, DiagnosticCode code, Object... details);

    /// Reports an error derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(Token token, DiagnosticCode code, Object... details);

    /// Reports an error derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param cause The cause of this report.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(Token token, Throwable cause, DiagnosticCode code, Object... details);

    /// Reports a [LocalizableException]
    ///
    /// @param exception The exception to report.
    void error(LocalizableException exception);

    /// Reports a [LocalizableRuntimeException]
    ///
    /// @param exception The exception to report.
    void error(LocalizableRuntimeException exception);

    //
    // Deprecated Methods
    //

    /// @deprecated As of release 1.1.0, replaced by info(DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void info(String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method info(String, Object...) is deprecated. " +
            "Use info(DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by warn(DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void warn(String code, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method warn(String, Object...) is deprecated. " +
            "Use warn(DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by error(DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void error(String code, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method error(String, Object...) is deprecated. " +
            "Use error(Throwable, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by infoAt(int, int, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void infoAt(int line, int column, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method infoAt(int, int, String, Object...) is deprecated. " +
            "Use infoAt(int, int, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by infoAt(int, int, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void warnAt(int line, int column, String code, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method warnAt(int, int, String, Object...) is deprecated. " +
            "Use warnAt(int, int, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by infoAt(int, int, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void errorAt(int line, int column, String code, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method errorAt(int, int, String, Object...) is deprecated. " +
            "Use errorAt(int, int, Throwable, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by infoAt(int, int, int, int, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void infoAt(int line, int column, int start, int end, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method infoAt(int, int, int, int, String, Object...) is deprecated. " +
            "Use infoAt(int, int, int, int, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by infoAt(int, int, int, int, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void warnAt(int line, int column, int start, int end, String code, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method warnAt(int, int, int, int, String, Object...) is deprecated. " +
            "Use warnAt(int, int, int, int, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by infoAt(int, int, int, int, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void errorAt(int line, int column, int start, int end, String code, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method errorAt(int, int, int, int, String, Object...) is deprecated. " +
            "Use errorAt(int, int, int, int, Throwable, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by info(Token, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void infoAt(Token token, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method infoAt(Token, String, Object...) is deprecated. " +
            "Use infoAt(Token, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by info(Token, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void warnAt(Token token, String code, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method warnAt(Token, String, Object...) is deprecated. " +
            "Use warnAt(Token, DiagnosticCode, Object...) instead."
        );
    }

    /// @deprecated As of release 1.1.0, replaced by info(Token, DiagnosticCode, Object...)
    @Deprecated(forRemoval = true)
    default void errorAt(Token token, String code, String format, Object... args) {
        throw new UnsupportedOperationException(
            "Method errorAt(Token, String, Object...) is deprecated. " +
            "Use errorAt(Token, Throwable, DiagnosticCode, Object...) instead."
        );
    }
}