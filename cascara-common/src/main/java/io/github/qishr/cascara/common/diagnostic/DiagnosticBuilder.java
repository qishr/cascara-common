package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.lang.token.Token;

public class DiagnosticBuilder {
    /// With message string
    public static Diagnostic build(String source, Level level, String format, Object... details) {
        return new Diagnostic(
            null,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            Diagnostic.UNKNOWN_COORD,
            source, level, null, null, format, details
        );
    }

    /// With diagnostic code, and cause
    public static Diagnostic build(String source, Level level, Throwable cause, DiagnosticMessage code, Object... details) {
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
    public static Diagnostic build(URI uri, int line, int column, int startOffset, int endOffset, String source, Level level, Throwable cause, DiagnosticMessage code, Object... details) {
        return new Diagnostic(uri, line, column, startOffset, endOffset, source, level, cause, code, null, details);
    }

    /// With diagnostic code, token, and cause
    public static Diagnostic build(Token token, String source, Level level, Throwable cause, DiagnosticMessage code, Object... details) {
        if (token == null) {
            throw new IllegalArgumentException("Token must not be null");
        }
        return new Diagnostic(null, token, source, level, cause, code, null, details);
    }
}
