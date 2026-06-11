package io.github.qishr.cascara.common.diagnostic;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public class LocalizableException extends Exception {
    private static volatile DiagnosticLocalizer localizer = DiagnosticLocalizer.DEFAULT;

    private final DiagnosticCode code;
    private final Object[] details;

    public static void setLocalizer(DiagnosticLocalizer customLocalizer) {
        localizer = customLocalizer != null ? customLocalizer : DiagnosticLocalizer.DEFAULT;
    }

    public LocalizableException(DiagnosticCode code, Object... details) {
        super(format(code, details));
        this.code = code;
        this.details = details != null ? details : new Object[0];
    }

    public LocalizableException(Throwable cause, DiagnosticCode code, Object... details) {
        super(format(code, details));
        this.code = code;
        this.details = details != null ? details : new Object[0];
    }

    /// Returns a diagnostic error code for the error message.
	public DiagnosticCode getCode() {
		return code;
	}

    /// Returns the details, if any, to be used in formatting the error message.
	public Object[] getDetails() {
		return details;
	}

    /// Returns a localized, formatted error message.
    @Override
    public String getLocalizedMessage() {
        try {
            return localizer.format(code, details);
        } catch (IllegalArgumentException e) {
            return String.format(DiagnosticLocalizer.FORMATTING_ERROR, code.getCode(), code.getMessage());
        }
    }

    /// Formats a [DiagnosticCode]'s message without localizing it.
    private static String format(DiagnosticCode code, Object... details) {
        return DiagnosticLocalizer.DEFAULT.format(code, details);
    }
}
