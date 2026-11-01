package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;


public class SignatureException extends LocalizableSecurityException {
    private final SignatureStatus signatureStatus;

    public SignatureException(SignatureStatus signatureStatus, DiagnosticMessage message, Object... details) {
        super(message, details);
        this.signatureStatus = signatureStatus;
    }

    public SignatureException(Throwable t, SignatureStatus signatureStatus, DiagnosticMessage message, Object... details) {
        super(message, details, t);
        this.signatureStatus = signatureStatus;
    }

    public SignatureStatus getSignatureStatus() {
        return signatureStatus;
    }
}