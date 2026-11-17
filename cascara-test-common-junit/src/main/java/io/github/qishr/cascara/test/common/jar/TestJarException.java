package io.github.qishr.cascara.test.common.jar;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.test.common.util.TestException;

public class TestJarException extends TestException {
    public TestJarException(LocalizableMessage msg, Object... details) {
        super(msg, details);
    }
    public TestJarException(Throwable t, LocalizableMessage msg, Object... details) {
        super(t, msg, details);
    }
    public TestJarException(Throwable t, String msg) {
        super(t, GenericMessage.ERROR, msg);
    }
    public TestJarException(String msg) {
        super(GenericMessage.ERROR, msg);
    }
}
