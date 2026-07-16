package io.github.qishr.cascara.common.lang.exception;

import io.github.qishr.cascara.common.diagnostic.AbstractLocalizableException;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.lang.annotation.Experimental;

@Experimental
public class JsonPointerException extends AbstractLocalizableException {

	public JsonPointerException(DiagnosticCode code, Object... details) {
		super(code, details);
	}

	public JsonPointerException(Throwable cause, DiagnosticCode code, Object... details) {
		super(cause, code, details);
	}

}
