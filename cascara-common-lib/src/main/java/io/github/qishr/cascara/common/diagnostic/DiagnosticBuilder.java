package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.lang.token.Token;

@Experimental
public class DiagnosticBuilder {
    /// With format string. This is used for debug and trace reports. They do not carry a stack trace or cause.
    public static Diagnostic build(String source, Level level, String format, Object... details) {
        return new Diagnostic(
            null,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, level, null, null, null, format, details
        );
    }

    /// With diagnostic message, and cause
    public static Diagnostic build(String source, Level level, StackTraceElement[] stackTrace, Throwable cause, DiagnosticMessage message, Object... details) {
        return new Diagnostic(
            null,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, level, stackTrace, cause, message, null, details
        );
    }

    /// With diagnostic message, location, and cause
    public static Diagnostic build(URI uri, int line, int column, int startOffset, int endOffset, String source, Level level, StackTraceElement[] stackTrace, Throwable cause, DiagnosticMessage message, Object... details) {
        return new Diagnostic(uri, line, column, startOffset, endOffset, source, level, stackTrace, cause, message, null, details);
    }

    /// With diagnostic message, token, and cause
    public static Diagnostic build(Token token, String source, Level level, StackTraceElement[] stackTrace, Throwable cause, DiagnosticMessage message, Object... details) {
        if (token == null) {
            throw new IllegalArgumentException("Token must not be null");
        }
        return new Diagnostic(null, token, source, level, stackTrace, cause, message, null, details);
    }
}
