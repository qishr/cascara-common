package io.github.qishr.cascara.test.common.jar;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class TestJarException extends LocalizableRuntimeException {
    public TestJarException(DiagnosticMessage msg, Object... details) {
        super(msg, details);
    }
    public TestJarException(Throwable t, DiagnosticMessage msg, Object... details) {
        super(t, msg, details);
    }
}
