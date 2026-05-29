package io.github.qishr.cascara.common.lang.exception;

import java.net.URI;

import io.github.qishr.cascara.common.util.CascaraRuntimeException;

public abstract class LocatableException extends CascaraRuntimeException {
    // Common constants for LocatableException implementations
    public static final int UNKNOWN_COORD = -1;

    private final int line;
    private final int column;
    private final URI uri;
    private final String rawMessage;

    // Standard constructor for parser-detected logic errors
    public LocatableException(String message, int line, int column, URI uri) {
        super(messageWithLocation(message, line, uri));
        this.rawMessage = message;
        this.line = line;
        this.column = column;
        this.uri = uri;
    }

    // The "Wrapper" constructor for I/O or Stream failures
    public LocatableException(String message, Throwable cause, int line, int column, URI uri) {
        super(messageWithLocation(message, line, uri));
        this.rawMessage = message;
        this.line = line;
        this.column = column;
        this.uri = uri;
    }

    // Constructor for when we only have a URI (e.g. File not found)
    public LocatableException(String message, Throwable cause, URI uri) {
        this(message, cause, UNKNOWN_COORD, UNKNOWN_COORD, uri);
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
    public String getRawMessage() { return rawMessage; }
}