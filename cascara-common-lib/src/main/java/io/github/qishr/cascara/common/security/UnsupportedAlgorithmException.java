package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class UnsupportedAlgorithmException extends SignatureException {
    private static final DiagnosticMessage message = SecurityMessage.UNSUPPORTED_ALGORITHM;
    private static final SignatureStatus status = SignatureStatus.UNSUPPORTED_ALGORITHM;

    public UnsupportedAlgorithmException(String algorithm) {
        super(status, message, algorithm);
    }

    public UnsupportedAlgorithmException(Throwable cause, String algorithm) {
        super(cause, status, message, algorithm);
    }
}