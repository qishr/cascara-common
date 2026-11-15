package io.github.qishr.cascara.common.i18n.exception;

import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.i18n.Localizer;

public class LocalizableSecurityException extends SecurityException implements LocalizableException {

    private final LocalizableMessage localizableMessage;
    private final Object[] details;

    public LocalizableSecurityException(LocalizableMessage message, Object... details) {
        super(Localizer.instance().formatException(message, details));
        this.localizableMessage = message;
        this.details = details != null ? details : new Object[0];
    }

    public LocalizableSecurityException(Throwable cause, LocalizableMessage message, Object... details) {
        super(Localizer.instance().formatException(message, details), cause);
        this.localizableMessage = message;
        this.details = details != null ? details : new Object[0];
    }

    /// {@inheritDoc}
    @Override
    public LocalizableMessage getLocalizableMessage() {
		return localizableMessage;
	}

    /// {@inheritDoc}
    @Override
	public Object[] getDetails() {
		return details;
	}

    /// {@inheritDoc}
    @Override
    public String getLocalizedMessage() {
        return Localizer.instance().format(localizableMessage, details);
    }

    /// {@inheritDoc}
    @Override
    public String getMessage() {
        return Localizer.instance().formatException(localizableMessage, details);
    }
}