package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class LocalizableNoSuchAlgorithmException extends SignatureException {
    private static final DiagnosticMessage message = SecurityMessage.NO_SUCH_ALGORITHM;
    private static final SignatureStatus status = SignatureStatus.UNSUPPORTED_ALGORITHM;

    public LocalizableNoSuchAlgorithmException(String algorithm) {
        super(status, message, algorithm);
    }

    public LocalizableNoSuchAlgorithmException(Throwable cause, String algorithm) {
        super(cause, status, message, algorithm);
    }
}