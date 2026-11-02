package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class UnsupportedDigestException extends SignatureException {
    private static final DiagnosticMessage message = SecurityMessage.UNSUPPORTED_DIGEST;
    private static final SignatureStatus status = SignatureStatus.UNSUPPORTED_DIGEST;

    public UnsupportedDigestException(String algorithm) {
        super(status, message, algorithm);
    }

    public UnsupportedDigestException(Throwable cause, String algorithm) {
        super(cause, status, message, algorithm);
    }
}