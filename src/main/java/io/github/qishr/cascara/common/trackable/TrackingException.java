package io.github.qishr.cascara.common.trackable;

import io.github.qishr.cascara.common.diagnostic.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public class TrackingException extends LocalizableRuntimeException {
    /// Constructor for errors without an exception.
    public TrackingException(DiagnosticCode code, Object... details) {
        super(code, details);
    }

    /// Constructor for errors with an exception.
    public TrackingException(Throwable cause, DiagnosticCode code, Object... details) {
        super(cause, code, details);
    }
}
