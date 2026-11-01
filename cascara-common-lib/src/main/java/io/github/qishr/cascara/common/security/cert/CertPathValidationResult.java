package io.github.qishr.cascara.common.security.cert;

import java.security.cert.CertPath;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Optional;

public final class CertPathValidationResult {

    private final boolean valid;
    private final CertPath certPath;
    private final TrustAnchor trustAnchor;
    private final String errorMessage;
    private final Throwable cause;

    private CertPathValidationResult(
            boolean valid,
            CertPath certPath,
            TrustAnchor trustAnchor,
            String errorMessage,
            Throwable cause) {
        this.valid = valid;
        this.certPath = certPath;
        this.trustAnchor = trustAnchor;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    public static CertPathValidationResult valid(CertPath certPath, TrustAnchor trustAnchor) {
        return new CertPathValidationResult(true, certPath, trustAnchor, null, null);
    }

    public static CertPathValidationResult invalid(String errorMessage, Throwable cause) {
        return new CertPathValidationResult(false, null, null, errorMessage, cause);
    }

    public static CertPathValidationResult invalid(String errorMessage) {
        return invalid(errorMessage, null);
    }

    public boolean isValid() {
        return valid;
    }

    public Optional<CertPath> getCertPath() {
        return Optional.ofNullable(certPath);
    }

    public Optional<TrustAnchor> getTrustAnchor() {
        return Optional.ofNullable(trustAnchor);
    }

    public Optional<X509Certificate> getTrustAnchorCertificate() {
        return Optional.ofNullable(trustAnchor != null ? trustAnchor.getTrustedCert() : null);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public Optional<Throwable> getCause() {
        return Optional.ofNullable(cause);
    }
}