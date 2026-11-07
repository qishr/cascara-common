package io.github.qishr.cascara.common.diagnostic.exception;

import io.github.qishr.cascara.common.diagnostic.DetailedMessage;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class LocalizableInstantiationException extends InstantiationException implements LocalizableException {
    private final Throwable cause;
    private final Class<?> targetClass;
    private final DetailedMessage detailedMessage;

    public LocalizableInstantiationException(Throwable cause, Class<?> targetClass) {
        super(LocalizableException.formatMessage(DetailedMessage.forException(cause)));
        this.cause = cause;
        this.targetClass = targetClass;
        this.detailedMessage = DetailedMessage.forException(cause);
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    // TODO: Change this to return LocalizableMessage or DetailedMessage
    @Override
    public DiagnosticMessage getDiagnosticMessage() {
		// return diagnosticMessage;
        if (detailedMessage.getMessage() instanceof DiagnosticMessage dm) {
            return dm;
        }
        return null;
	}

    @Override
	public Object[] getDetails() {
		return detailedMessage.getDetails();
	}

    @Override
    public String getLocalizedMessage() {
        return LocalizableException.formatMessage(detailedMessage);
    }
}