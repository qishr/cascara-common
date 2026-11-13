package io.github.qishr.cascara.common.security.diagnostic;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.diagnostic.message.LocalizableMessage;

public class TrustStoreException extends LocalizableSecurityException {

    public TrustStoreException(LocalizableMessage message, Object... details) {
        super(message, details);
    }

    public TrustStoreException(Throwable t, LocalizableMessage message, Object... details) {
        super(t, message, details);
    }

}
