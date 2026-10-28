package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class LocalizableSecurityException extends LocalizableRuntimeException {

    public LocalizableSecurityException(DiagnosticMessage message, Object... details) {
        super(message, details);
    }

    public LocalizableSecurityException(Throwable t, DiagnosticMessage message, Object... details) {
        super(t, message, details);
    }

}
