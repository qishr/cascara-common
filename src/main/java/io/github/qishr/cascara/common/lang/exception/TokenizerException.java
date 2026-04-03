package io.github.qishr.cascara.common.lang.exception;

import java.net.URI;

public class TokenizerException extends LanguageException {

    /// Standard constructor for parser-detected logic errors
    public TokenizerException(String message, int line, int column, URI uri) {
        super(message, line, column, uri);
    }

    /// The "Wrapper" constructor for I/O or Stream failures
    public TokenizerException(String message, Throwable cause, int line, int column, URI uri) {
        super(message, cause, line, column, uri);
    }

    /// Constructor for when we only have a URI (e.g. File not found)
    public TokenizerException(String message, Throwable cause, URI uri) {
        this(message, cause, UNKNOWN_COORD, UNKNOWN_COORD, uri);
    }
}