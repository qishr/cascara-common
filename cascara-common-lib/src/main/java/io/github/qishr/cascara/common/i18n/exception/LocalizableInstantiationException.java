package io.github.qishr.cascara.common.i18n.exception;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;

public class LocalizableInstantiationException extends InstantiationException implements LocalizableException {
    private final Throwable cause;
    private final DiagnosticMessage detailedMessage;

    public LocalizableInstantiationException(Throwable cause) {
        super(LocalizableException.formatMessage(DiagnosticMessage.forException(cause)));
        this.cause = cause;
        this.detailedMessage = DiagnosticMessage.forException(cause);
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public LocalizableMessage getLocalizableMessage() {
        return detailedMessage;
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