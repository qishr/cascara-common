package io.github.qishr.cascara.common.diagnostic.exception;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public abstract class LocalizableSecurityException extends LocalizableRuntimeException {

    public LocalizableSecurityException(DiagnosticMessage message, Object... details) {
        super(message, details);
    }

    public LocalizableSecurityException(Throwable t, DiagnosticMessage message, Object... details) {
        super(t, message, details);
    }

}
