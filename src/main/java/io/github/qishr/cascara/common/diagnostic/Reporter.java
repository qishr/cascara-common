package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.lang.token.Token;

public interface Reporter {

    /// Sets the level of output when logging directly to the console.
    Reporter setLevel(Level level);

    Reporter setDiagnosticCollector(Consumer<Diagnostic> collector);

    Reporter setProblemCollector(Consumer<Diagnostic> collector);

    boolean collectsProblems();

    /// Reports an trace message through the reporter.
    /// @param format The format of the message to report.
    /// @param args Arguments referenced by the format specifiers in the format string.
    /// If the last argument is a Throwable, it will not be used in message formatting
    /// and will instead become a field in the Diagnostic produced by this call.
    void trace(String format, Object... args);

    /// Reports an debug message through the reporter.
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
    /// @param format The format of the message to report.
    /// @param args Arguments referenced by the format specifiers in the format string.
    /// If the last argument is a Throwable, it will not be used in message formatting
    /// and will instead become a field in the Diagnostic produced by this call.
    void warn(String format, Object... args);

    /// Reports an error message including location information.
    /// @param format The format of the message to report.
    /// @param args Arguments referenced by the format specifiers in the format string.
    /// If the last argument is a Throwable, it will not be used in message formatting
    /// and will instead become a field in the Diagnostic produced by this call.
    void error(String format, Object... args);

    void infoAt(int start, int end, int line, int column, URI uri, String format, Object... args);
    void warnAt(int start, int end, int line, int column, URI uri, String format, Object... args);
    void errorAt(int start, int end, int line, int column, URI uri, String format, Object... args);

    void infoAt(Token token, URI uri, String format, Object... args);
    void warnAt(Token token, URI uri, String format, Object... args);
    void errorAt(Token token, URI uri, String format, Object... args);
}