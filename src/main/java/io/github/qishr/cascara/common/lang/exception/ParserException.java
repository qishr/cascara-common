package io.github.qishr.cascara.common.lang.exception;

import io.github.qishr.cascara.common.diagnostic.LocatableException;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.lang.token.Token;

public class ParserException extends LocatableException {

    /// Standard constructor for parser-detected logic errors.
    public ParserException(int line, int column, DiagnosticCode code, Object... details) {
        super(null, line, column, code, details);
    }

    /// Standard constructor for parser-detected logic errors.
    public ParserException(Token token, DiagnosticCode code, Object... details) {
        super(null, token.getStartLine(), token.getStartColumn(), code, details);
    }

    /// Constructor for I/O or Stream failures.
    public ParserException(Throwable cause, DiagnosticCode code, Object... details) {
        super(null, UNKNOWN_COORD, UNKNOWN_COORD, cause, code, details);
    }
}