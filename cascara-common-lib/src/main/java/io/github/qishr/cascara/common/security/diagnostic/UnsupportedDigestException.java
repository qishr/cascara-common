package io.github.qishr.cascara.common.security.diagnostic;

import io.github.qishr.cascara.common.diagnostic.message.LocalizableMessage;
import io.github.qishr.cascara.common.security.policy.SignatureException;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;

public class UnsupportedDigestException extends SignatureException {
    private static final LocalizableMessage message = SecurityMessage.UNSUPPORTED_DIGEST;
    private static final SignatureStatus status = SignatureStatus.UNSUPPORTED_ALGORITHM;

    public UnsupportedDigestException(String algorithm) {
        super(status, message, algorithm);
    }

    public UnsupportedDigestException(Throwable cause, String algorithm) {
        super(cause, status, message, algorithm);
    }
}