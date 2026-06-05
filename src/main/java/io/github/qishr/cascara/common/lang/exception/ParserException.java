package io.github.qishr.cascara.common.lang.exception;

public class ParserException extends LocatableException {

    // Standard constructor for parser-detected logic errors
    public ParserException(String message, int line, int column) {
        super(message, line, column, null);
    }

    // Constructor for I/O or Stream failures
    public ParserException(String message, Throwable cause) {
        super(message, cause, UNKNOWN_COORD, UNKNOWN_COORD, null);
    }
}