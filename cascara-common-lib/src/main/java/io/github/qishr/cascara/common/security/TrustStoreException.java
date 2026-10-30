package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class TrustStoreException extends LocalizableSecurityException {

    public TrustStoreException(DiagnosticMessage message, Object... details) {
        super(message, details);
    }

    public TrustStoreException(Throwable t, DiagnosticMessage message, Object... details) {
        super(t, message, details);
    }

}
