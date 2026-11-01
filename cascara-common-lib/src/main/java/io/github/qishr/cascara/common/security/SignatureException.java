package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.diagnostic.exception.UnexpectedNullParameterException;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;


public class SignatureException extends LocalizableSecurityException {
    @NonNull private final SignatureStatus signatureStatus;

    public SignatureException(SignatureStatus signatureStatus, DiagnosticMessage message, Object... details) {
        super(message, details);
        if (signatureStatus == null) {
            throw new UnexpectedNullParameterException("signatureStatus");
        }
        this.signatureStatus = signatureStatus;
    }

    public SignatureException(Throwable t, SignatureStatus signatureStatus, DiagnosticMessage message, Object... details) {
        super(message, details, t);
        if (signatureStatus == null) {
            throw new UnexpectedNullParameterException("signatureStatus");
        }
        this.signatureStatus = signatureStatus;
    }

    @NonNull
    public SignatureStatus getSignatureStatus() {
        return signatureStatus;
    }
}