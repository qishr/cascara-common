package io.github.qishr.cascara.common.lang.exception;

import java.net.URI;

public class ParserException extends LocatableException {

    // Standard constructor for parser-detected logic errors
    public ParserException(String message, int line, int column, URI uri) {
        super(message, line, column, uri);
    }

    // Constructor for I/O or Stream failures
    public ParserException(String message, Throwable cause, URI uri) {
        super(message, cause, UNKNOWN_COORD, UNKNOWN_COORD, uri);
    }
}