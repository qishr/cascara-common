package io.github.qishr.cascara.common.security.exception;

import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.i18n.exception.LocalizableSecurityException;

public class TrustStoreException extends LocalizableSecurityException {

    public TrustStoreException(LocalizableMessage message, Object... details) {
        super(message, details);
    }

    public TrustStoreException(Throwable t, LocalizableMessage message, Object... details) {
        super(t, message, details);
    }

}
