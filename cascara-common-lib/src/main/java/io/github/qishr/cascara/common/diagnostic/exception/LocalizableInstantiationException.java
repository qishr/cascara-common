package io.github.qishr.cascara.common.diagnostic.exception;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class LocalizableInstantiationException extends InstantiationException implements LocalizableException {

    private final DiagnosticMessage diagnosticMessage;
    private final Object[] details;
    private final Throwable cause;

    public LocalizableInstantiationException(Throwable cause, Class<?> cls) {
        super(LocalizableException.formatMessage(DiagnosticMessage.forException(cause), new Object[]{cls}));
        this.cause = cause;
        this.diagnosticMessage = DiagnosticMessage.forException(cause);
        this.details = new Object[]{cls};
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public DiagnosticMessage getDiagnosticMessage() {
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