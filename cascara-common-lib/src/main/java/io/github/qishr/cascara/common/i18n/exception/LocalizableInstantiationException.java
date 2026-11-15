package io.github.qishr.cascara.common.i18n.exception;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.i18n.Localizer;

public class LocalizableInstantiationException extends InstantiationException implements LocalizableException {
    private final Throwable cause;
    private final DiagnosticMessage detailedMessage;

    public LocalizableInstantiationException(Throwable cause) {
        super(Localizer.instance().formatException(DiagnosticMessage.forException(cause)));
        this.cause = cause;
        this.detailedMessage = DiagnosticMessage.forException(cause);
    }

    /// {@inheritDoc}
    @Override
    public Throwable getCause() {
        return cause;
    }

    /// {@inheritDoc}
    @Override
    public LocalizableMessage getLocalizableMessage() {
        return detailedMessage;
	}

    /// {@inheritDoc}
    @Override
	public Object[] getDetails() {
		return detailedMessage.getDetails();
	}

    /// {@inheritDoc}
    @Override
    public String getLocalizedMessage() {
        return Localizer.instance().format(detailedMessage);
    }

    /// {@inheritDoc}
    @Override
    public String getMessage() {
        return Localizer.instance().formatException(detailedMessage);
    }
}