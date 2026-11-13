package io.github.qishr.cascara.common.diagnostic.exception;

import io.github.qishr.cascara.common.diagnostic.DetailedMessage;
import io.github.qishr.cascara.common.diagnostic.message.LocalizableMessage;

public class LocalizableInstantiationException extends InstantiationException implements LocalizableException {
    private final Throwable cause;
    private final DetailedMessage detailedMessage;

    public LocalizableInstantiationException(Throwable cause) {
        super(LocalizableException.formatMessage(DetailedMessage.forException(cause)));
        this.cause = cause;
        this.detailedMessage = DetailedMessage.forException(cause);
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