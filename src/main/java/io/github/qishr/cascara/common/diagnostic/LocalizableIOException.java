package io.github.qishr.cascara.common.diagnostic;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public class LocalizableIOException extends LocalizableException {
    public LocalizableIOException(DiagnosticCode code, Object... details) {
        super(code, details);
    }

    public LocalizableIOException(Throwable cause, DiagnosticCode code, Object... details) {
        super(cause, code, details);
    }
}
