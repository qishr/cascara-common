package io.github.qishr.cascara.common.lang.exception;

import io.github.qishr.cascara.common.diagnostic.LocalizableException;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public class JsonPointerException extends LocalizableException {

	public JsonPointerException(DiagnosticCode code, Object... details) {
		super(code, details);
	}

	public JsonPointerException(Throwable cause, DiagnosticCode code, Object... details) {
		super(cause, code, details);
	}

}
