package io.github.qishr.cascara.common.lang.exception;

import io.github.qishr.cascara.common.diagnostic.LocatableException;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public class TokenizerException extends LocatableException {

    /// Standard constructor for tokenizer-detected logic errors.
    public TokenizerException(int line, int column, DiagnosticCode code, Object... details) {
        super(null, line, column, code, details);
    }

    /// Constructor for I/O or Stream failures.
    public TokenizerException(Throwable cause, DiagnosticCode code, Object... details) {
        super(null, UNKNOWN_COORD, UNKNOWN_COORD, cause, code, details);
    }
}