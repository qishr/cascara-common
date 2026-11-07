package io.github.qishr.cascara.common.diagnostic.exception;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class LocalizableSecurityException extends SecurityException implements LocalizableException {

    private final DiagnosticMessage diagnosticMessage;
    private final Object[] details;

    public LocalizableSecurityException(DiagnosticMessage message, Object... details) {
        super(LocalizableException.formatMessage(message, details));
        this.diagnosticMessage = message;
        this.details = details != null ? details : new Object[0];
    }

    public LocalizableSecurityException(Throwable cause, DiagnosticMessage message, Object... details) {
        super(LocalizableException.formatMessage(message, details), cause);
        this.diagnosticMessage = message;
        this.details = details != null ? details : new Object[0];
    }

    @Override
    public DiagnosticMessage getLocalizableMessage() {
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