package io.github.qishr.cascara.common.security.diagnostic;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.security.policy.SignatureException;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;

public class InvalidKeyException extends SignatureException {
    private static final DiagnosticMessage message = SecurityMessage.INVALID_KEY;
    private static final SignatureStatus status = SignatureStatus.INVALID_KEY;

    public InvalidKeyException(String key) {
        super(status, message, key);
    }

    public InvalidKeyException(Throwable cause, String key) {
        super(cause, status, message, key);
    }
}