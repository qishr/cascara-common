package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class SecurityException extends LocalizableRuntimeException {

    public SecurityException(DiagnosticMessage message, Object... details) {
        super(message, details);
    }

    public SecurityException(Throwable t, DiagnosticMessage message, Object... details) {
        super(t, message, details);
    }

}
