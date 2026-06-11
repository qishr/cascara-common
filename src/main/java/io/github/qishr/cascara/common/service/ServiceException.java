package io.github.qishr.cascara.common.service;

import io.github.qishr.cascara.common.diagnostic.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;

public class ServiceException extends LocalizableRuntimeException {
    /// Constructor for errors without an exception
    public ServiceException(DiagnosticCode code, Object... details) {
        super(null, code, details);
    }

    /// Constructor for errors with an exception
    public ServiceException(Throwable cause, DiagnosticCode code, Object... details) {
        super(cause, code, details);
    }

    /// @deprecated  As of release 1.1.0
    @Deprecated(forRemoval = true)
    public ServiceException(String message) {
        super(GenericDiagnosticCode.ERROR, message);
    }

    /// @deprecated  As of release 1.1.0
    @Deprecated(forRemoval = true)
    public ServiceException(String message, Throwable e) {
        super(e, GenericDiagnosticCode.ERROR, message);
    }
}
