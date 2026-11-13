package io.github.qishr.cascara.common.security.policy;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.diagnostic.exception.UnexpectedNullParameterException;
import io.github.qishr.cascara.common.diagnostic.message.LocalizableMessage;

public class SignatureException extends LocalizableSecurityException {
    @NonNull private final SignatureStatus signatureStatus;

    public SignatureException(SignatureStatus signatureStatus, LocalizableMessage message, Object... details) {
        super(message, details);
        if (signatureStatus == null) {
            throw new UnexpectedNullParameterException("signatureStatus");
        }
        this.signatureStatus = signatureStatus;
    }

    public SignatureException(Throwable t, SignatureStatus signatureStatus, LocalizableMessage message, Object... details) {
        super(t, message, details);
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