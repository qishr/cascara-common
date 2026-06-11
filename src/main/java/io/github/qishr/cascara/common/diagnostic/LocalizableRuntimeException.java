package io.github.qishr.cascara.common.diagnostic;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public class LocalizableRuntimeException extends RuntimeException {
    private static volatile DiagnosticLocalizer localizer = DiagnosticLocalizer.DEFAULT;

    private final DiagnosticCode code;
    private final Object[] details;

    public static void setLocalizer(DiagnosticLocalizer customLocalizer) {
        localizer = customLocalizer != null ? customLocalizer : DiagnosticLocalizer.DEFAULT;
    }

    public LocalizableRuntimeException(DiagnosticCode code, Object... details) {
        super(localizer.format(code, details));
        this.code = code;
        this.details = details != null ? details : new Object[0];
    }

    public LocalizableRuntimeException(Throwable cause, DiagnosticCode code, Object... details) {
        super(localizer.format(code, details));
        this.code = code;
        this.details = details != null ? details : new Object[0];
    }

	public DiagnosticCode getCode() {
		return code;
	}

	public Object[] getDetails() {
		return details;
	}

    @Override
    public String getLocalizedMessage() {
        return localizer.format(code, details);
    }
}
