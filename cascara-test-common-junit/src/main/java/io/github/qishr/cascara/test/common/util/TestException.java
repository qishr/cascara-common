package io.github.qishr.cascara.test.common.util;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.i18n.exception.LocalizableRuntimeException;

public class TestException extends LocalizableRuntimeException {
    public TestException(LocalizableMessage msg, Object... details) {
        super(msg, details);
    }
    public TestException(Throwable t, LocalizableMessage msg, Object... details) {
        super(t, msg, details);
    }
    public TestException(Throwable t, String msg) {
        super(t, GenericMessage.ERROR, msg);
    }
    public TestException(String msg) {
        super(GenericMessage.ERROR, msg);
    }
}
