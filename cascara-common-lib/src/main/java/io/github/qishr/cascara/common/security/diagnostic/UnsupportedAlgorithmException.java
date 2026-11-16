package io.github.qishr.cascara.common.security.diagnostic;

import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.security.policy.SignatureException;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;

public class UnsupportedAlgorithmException extends SignatureException {
    private static final LocalizableMessage message = SecurityMessage.UNSUPPORTED_ALGORITHM;
    private static final SignatureStatus status = SignatureStatus.UNSUPPORTED_ALGORITHM;

    public UnsupportedAlgorithmException(String algorithm) {
        super(status, message, algorithm);
    }

    public UnsupportedAlgorithmException(Throwable cause, String algorithm) {
        super(cause, status, message, algorithm);
    }
}