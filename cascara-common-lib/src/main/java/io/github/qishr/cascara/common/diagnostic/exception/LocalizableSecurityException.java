package io.github.qishr.cascara.common.diagnostic.exception;

import io.github.qishr.cascara.common.diagnostic.LocalizableMessage;

public class LocalizableSecurityException extends SecurityException implements LocalizableException {

    private final LocalizableMessage diagnosticMessage;
    private final Object[] details;

    public LocalizableSecurityException(LocalizableMessage message, Object... details) {
        super(LocalizableException.formatMessage(message, details));
        this.diagnosticMessage = message;
        this.details = details != null ? details : new Object[0];
    }

    public LocalizableSecurityException(Throwable cause, LocalizableMessage message, Object... details) {
        super(LocalizableException.formatMessage(message, details), cause);
        this.diagnosticMessage = message;
        this.details = details != null ? details : new Object[0];
    }

    @Override
    public LocalizableMessage getLocalizableMessage() {
		return diagnosticMessage;
	}

    @Override
	public Object[] getDetails() {
		return details;
	}

    @Override
    public String getLocalizedMessage() {
        return LocalizableException.formatMessage(diagnosticMessage, details);
    }
}