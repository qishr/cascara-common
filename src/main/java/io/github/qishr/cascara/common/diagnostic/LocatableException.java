package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public abstract class LocatableException extends LocalizableRuntimeException {
    // Common constants for LocatableException implementations
    public static final int UNKNOWN_COORD = -1;

    private final int line;
    private final int column;
    private final URI uri;
    private final String message;
    private final String rawMessage;

    /// Standard Constructor
    public LocatableException(URI uri, int line, int column, Throwable cause, DiagnosticCode code, Object... details) {
        super(cause, code, details);
        this.message = messageWithLocation(code.getMessage(), line, uri);
        this.rawMessage = code.getMessage();
        this.line = line;
        this.column = column;
        this.uri = uri;
    }

    /// Standard constructor for parser-detected logic errors
    public LocatableException(URI uri, int line, int column, DiagnosticCode code, Object... details) {
        this(uri, line, column, null, code, details);
    }

    /// Constructor for when we only have a URI but no line or column
    public LocatableException(URI uri, Throwable cause, DiagnosticCode code, Object... details) {
        this(uri, UNKNOWN_COORD, UNKNOWN_COORD, cause, code, details);
    }

    private static String messageWithLocation(String message, int line, URI uri) {
        if (uri == null) {
            return String.format("%s at line %d", message, line);
        } else {
            return String.format("%s at %s:%d", message, uri.toString(), line);
        }
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
    public URI getUri() { return uri; }
    public String getMessage() { return message; }
    public String getRawMessage() { return rawMessage; }

    @Override
    public String getLocalizedMessage() {
        String baseMessage = super.getLocalizedMessage();
        if (uri == null) {
            // TODO: i18n this
            return String.format("%s at line %d", baseMessage, line);
        } else {
            // TODO: i18n this
            return String.format("%s at %s:%d", baseMessage, uri.toString(), line);
        }
    }
}