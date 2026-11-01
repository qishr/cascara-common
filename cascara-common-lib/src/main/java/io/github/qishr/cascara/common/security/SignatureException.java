package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.exception.LocalizableException;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public class SignatureException extends SecurityException implements LocalizableException {

    private final SignatureStatus signatureStatus;
    private final DiagnosticMessage diagnosticMessage;
    private final Object[] details;

    public SignatureException(SignatureStatus signatureStatus, DiagnosticMessage message, Object... details) {
        super(LocalizableException.formatMessage(message, details));
        this.signatureStatus = signatureStatus;
        this.diagnosticMessage = message;
        this.details = details != null ? details : new Object[0];
    }

    public SignatureException(Throwable t, SignatureStatus signatureStatus, DiagnosticMessage message, Object... details) {
        super(LocalizableException.formatMessage(message, details), t);
        this.signatureStatus = signatureStatus;
        this.diagnosticMessage = message;
        this.details = details != null ? details : new Object[0];
    }

    public SignatureStatus getSignatureStatus() {
        return signatureStatus;
    }

    @Override
    public DiagnosticMessage getDiagnosticMessage() {
		return diagnosticMessage;
	}

    @Override
	public Object[] getDetails() {
		return details;
	}

    @Override
    public String getLocalizedMessage() {
        return LocalizableException.formatMessage(diagnosticMessage, details);
    }
}
