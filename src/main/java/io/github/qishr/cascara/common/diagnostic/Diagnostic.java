package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;
import java.text.MessageFormat;
import java.time.LocalDateTime;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.lang.token.Token;

/// Represents a discrete event, log entry, or syntax/semantic problem identified
/// during system runtime or source compilation.
///
/// Each diagnostic encapsulates precise positioning boundaries, metadata strings,
/// structural tracking indicators like thread name and execution timestamps, and
/// an optional underlying exception cause.
public class Diagnostic {
    private URI uri;
    private final int line;
    private final int column;
    private final int startOffset;
    private final int endOffset;

    private final String source;
    private final Level level;
    private final DiagnosticCode code;
    private final String message;
    private final Object[] details;
    private final Throwable cause;

    private final String thread;    // This can be taken from the Thread class. No param needed.
    private final LocalDateTime timestamp;

    /// Constructs a fully qualified [Diagnostic] entry with absolute location indicators.
    ///
    /// The [thread] and [timestamp] properties are implicitly initialized at the moment
    /// of construction using `Thread.currentThread` and `LocalDateTime.now` respectively.
    ///
    /// @param uri The URI identifying the originating source resource.
    /// @param line The 1-based line number where the diagnostic is anchored.
    /// @param column The 1-based column number where the diagnostic is anchored.
    /// @param startOffset The 0-based absolute character index indicating the start of the text span.
    /// @param endOffset The 0-based absolute character index indicating the end of the text span (exclusive).
    /// @param source A identifier string representing the subsystem or component that created this diagnostic.
    /// @param level The severity [Level] classification of this diagnostic.
    /// @param cause The cause of this report
    /// @param code An optional stable error or classification code, primarily useful for localization and documentation lookups.
    /// @param message The descriptive message explaining this diagnostic event.
    /// @param details Arguments referenced by the format specifiers in the format string.
    public Diagnostic(URI uri, int line, int column, int startOffset, int endOffset, String source, Level level, Throwable cause, DiagnosticCode code, String message, Object... details) {
        this.uri = uri;
        this.line = line;
        this.column = column;
        this.startOffset = startOffset;
        this.endOffset = endOffset;

        this.source = source;
        this.level = level;
        this.cause = cause;
        this.code = code;
        this.details = details;

        this.thread = Thread.currentThread().getName();
        this.timestamp = LocalDateTime.now();

        String formattedMessage;

        if (message == null) {
            if (code == null) {
                formattedMessage = ("Message code or text required when creating Diagnostic.");
            } else {
                try {
                    formattedMessage = MessageFormat.format(code.getMessage(), details);
                } catch (IllegalArgumentException e) {
                    formattedMessage = "Formatting problem while reporting error code " + code.getCode() + ": " + code.getMessage() + ".";
                }
            }
        } else {
            try {
                formattedMessage = String.format(message, details);
            } catch (IllegalArgumentException e) {
                formattedMessage = "Formatting problem while reporting error: " + message + ".";
            }
        }

        this.message = formattedMessage;
    }

    /// Constructs a [Diagnostic] entry by resolving positioning metadata directly from a parsing [Token].
    ///
    /// The character range is calculated automatically by anchoring at the token's
    /// absolute index offset and extending the duration by its lexeme text span length.
    ///
    /// @param uri The URI identifying the originating source resource.
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param source A identifier string representing the subsystem or component that created this diagnostic.
    /// @param level The severity [Level] classification of this diagnostic.
    /// @param cause The cause of this report
    /// @param code An optional stable error or classification code, primarily useful for localization and documentation lookups.
    /// @param message The descriptive message explaining this diagnostic event.
    /// @param details Arguments referenced by the format specifiers in the format string.
    public Diagnostic(URI uri, Token token, String source, Level level, Throwable cause, DiagnosticCode code, String message, Object... details) {
        this(
            uri,
            token.getStartLine(),
            token.getStartColumn(),
            token.getOffset(),
            token.getOffset() + token.getLexeme().length(),
            source,
            level,
            cause,
            code,
            message,
            details
        );
    }

    /// Returns the URI of the source resource associated with this diagnostic.
    public URI getUri() { return uri; }

    /// Returns the 1-based line number of this diagnostic.
    public int getLine() { return line; }

    /// Returns the 1-based column number of this diagnostic.
    public int getColumn() { return column; }

    /// Returns the 0-based absolute starting character offset of this diagnostic.
    public int getStartOffset() { return startOffset; }

    /// Returns the 0-based absolute ending character offset (exclusive) of this diagnostic.
    public int getEndOffset() { return endOffset; }

    /// Returns the identifier string of the subsystem that produced this diagnostic.
    public String getSource() { return source; }

    /// Returns the severity [Level] classification of this diagnostic.
    public Level getLevel() { return level; }

    /// Returns the stable classification code, suitable for localization and system filtering.
    public DiagnosticCode getCode() { return code; }

    /// Returns the formatted descriptive text message of this diagnostic.
    public String getMessage() { return message; }

    /// Returns the arguments for the message format placeholders.
    public Object[] getDetails() { return details; }

    /// Returns the underlying exception that caused this diagnostic, or `null` if none was provided.
    public Throwable getCause() { return cause; }

    /// Returns the name of the execution thread that instantiated this diagnostic instance.
    public String getThread() { return thread; }

    /// Returns the exact timestamp indicating when this diagnostic was instantiated.
    public LocalDateTime getTimestamp() { return timestamp; }

    /// Sets the URI of the source resource associated with this diagnostic.
    public void setUri(URI uri) { this.uri = uri; }

    /// Defines the severity hierarchy classifications available for diagnostic tracking.
    public enum Level {
        /// Default fallback fallback logging severity level.
        DEFAULT(0),
        /// Represents fatal or execution-halting structural failures.
        ERROR(1),
        /// Indicates non-fatal semantic irregularities or suspicious configurations.
        WARN(2),
        /// Standard operational metrics, progress records, or structural notices.
        INFO(3),
        /// High-fidelity tracing notes optimized for debugging workflows.
        DEBUG(4),
        /// Ultra-fine-grained system diagnostic traces.
        TRACE(5);

        private int level;

        Level(int level) {
            this.level = level;
        }

        /// Returns the raw integer ordinal configuration weight assigned to this severity tier level.
        public int getLevel() {
            return level;
        }
    }
}
