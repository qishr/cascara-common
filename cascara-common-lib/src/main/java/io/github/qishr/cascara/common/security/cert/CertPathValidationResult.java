package io.github.qishr.cascara.common.security.cert;

import java.security.cert.CertPath;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.DetailedMessage;
import io.github.qishr.cascara.common.diagnostic.message.LocalizableMessage;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;

@Experimental
public final class CertPathValidationResult {
    @NonNull private final SignatureStatus status;
    private final CertPath certPath;
    private final TrustAnchor trustAnchor;
    private final int failedIndex;
    private final X509Certificate failedCertificate;
    private final DetailedMessage message; // TODO: LocalizableMessage
    private final Throwable cause;

    private CertPathValidationResult(
            @NonNull SignatureStatus status,
            CertPath certPath,
            TrustAnchor trustAnchor,
            int failedIndex,
            X509Certificate failedCertificate,
            DetailedMessage message,
            Throwable cause) {
        this.status = status;
        this.certPath = certPath;
        this.trustAnchor = trustAnchor;
        this.failedIndex = failedIndex;
        this.failedCertificate = failedCertificate;
        this.message = message;
        this.cause = cause;
    }

    public static CertPathValidationResult valid(CertPath certPath, TrustAnchor trustAnchor) {
        return new CertPathValidationResult(
                SignatureStatus.VALID, certPath, trustAnchor, -1, null, null, null);
    }

    public static CertPathValidationResult invalid(
            @NonNull SignatureStatus status,
            int failedIndex,
            X509Certificate failedCertificate,
            DetailedMessage message,
            Throwable cause) {
        return new CertPathValidationResult(
                status, null, null, failedIndex, failedCertificate, message, cause);
    }

    public static CertPathValidationResult invalid(
            @NonNull SignatureStatus status,
            DetailedMessage message,
            Throwable cause) {
        return invalid(status, -1, null, message, cause);
    }

    public static CertPathValidationResult invalid(
            @NonNull SignatureStatus status,
            LocalizableMessage message) {
        return invalid(status, -1, null, new DetailedMessage(message), null);
    }

    public boolean isValid() {
        return status == SignatureStatus.VALID;
    }

    @NonNull
    public SignatureStatus getStatus() {
        return status;
    }

    public int getFailedIndex() {
        return failedIndex;
    }

    @Nullable
    public X509Certificate getFailedCertificate() {
        return failedCertificate;
    }

    @Nullable
    public DetailedMessage getMessage() {
        return message;
    }

    @Nullable
    public CertPath getCertPath() {
        return certPath;
    }

    @Nullable
    public TrustAnchor getTrustAnchor() {
        return trustAnchor;
    }

    @Nullable
    public X509Certificate getTrustAnchorCertificate() {
        return trustAnchor != null ? trustAnchor.getTrustedCert() : null;
    }

    @Nullable
    public Throwable getCause() {
        return cause;
    }
}