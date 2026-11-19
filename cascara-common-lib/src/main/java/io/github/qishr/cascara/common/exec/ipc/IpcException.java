package io.github.qishr.cascara.common.exec.ipc;

import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.i18n.exception.LocalizableRuntimeException;

public class IpcException extends LocalizableRuntimeException {
    public IpcException(LocalizableMessage message, Object... details) {
        super(message, details);
    }

    public IpcException(Throwable cause, LocalizableMessage message, Object... details) {
        super(cause, message, details);
    }
}
