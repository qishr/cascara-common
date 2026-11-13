package io.github.qishr.cascara.common.trackable;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.message.LocalizableMessage;

public class TrackingException extends LocalizableRuntimeException {
    /// Constructor for errors without an exception.
    public TrackingException(LocalizableMessage message, Object... details) {
        super(message, details);
    }

    /// Constructor for errors with an exception.
    public TrackingException(Throwable cause, LocalizableMessage message, Object... details) {
        super(cause, message, details);
    }
}
