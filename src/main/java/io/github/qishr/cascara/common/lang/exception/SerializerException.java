package io.github.qishr.cascara.common.lang.exception;

import io.github.qishr.cascara.common.diagnostic.LocatableException;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.lang.ast.AstNode;

public class SerializerException extends LocatableException {
    /// Standard constructor for serializer-detected mapping errors.
    public SerializerException(DiagnosticCode code, Object... details) {
        super(null, UNKNOWN_COORD, UNKNOWN_COORD, code, details);
    }

    /// Constructor
    public SerializerException(AstNode node, Throwable cause, DiagnosticCode code, Object... details) {
        super(null, node.getStartLine(), node.getStartColumn(), cause, code, details);
    }

    /// Constructor for I/O or Stream failures.
    public SerializerException(Throwable cause, DiagnosticCode code, Object... details) {
        super(null, UNKNOWN_COORD, UNKNOWN_COORD, cause, code, details);
    }
}
