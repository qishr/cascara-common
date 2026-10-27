package io.github.qishr.cascara.common.security;

// public class SignaturePolicy {
//     public boolean isTrustStoreEnforced() {

//     }

//     public TrustStore getTrustStore() {

//     }
// }

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Set;

import io.github.qishr.cascara.common.security.cms.SignatureStatus;

public final class SignaturePolicy {

    private final boolean requireSigned;
    private final boolean checkExpiry;
    private final TrustStore trustStore;
    private final Set<String> allowedDigestAlgorithms;

    private SignaturePolicy(Builder builder) {
        this.requireSigned = builder.requireSigned;
        this.checkExpiry = builder.checkExpiry;
        this.trustStore = builder.trustStore != null ? builder.trustStore : TrustStore.fromSystemProperties();
        this.allowedDigestAlgorithms = builder.allowedDigestAlgorithms;
    }

    public boolean isRequireSigned() {
        return requireSigned;
    }

    // TODO: Is this meant to be the same as the above?
    public boolean isTrustStoreEnforced() {
        return requireSigned;
    }


    public TrustStore getTrustStore() {
        return trustStore;
    }

    public SignatureStatus evaluate(X509Certificate signingCert, boolean signatureValid) {
        if (signingCert == null) {
            return requireSigned ? SignatureStatus.NOT_SIGNED : SignatureStatus.VALID;
        }

        if (!signatureValid) {
            return SignatureStatus.INVALID_SIGNATURE;
        }

        if (checkExpiry) {
            try {
                signingCert.checkValidity();
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                return SignatureStatus.CERTIFICATE_EXPIRED;
            }
        }

        if (!trustStore.getCertificates().isEmpty() && !trustStore.contains(signingCert)) {
            return SignatureStatus.UNTRUSTED_SIGNER;
        }

        return SignatureStatus.VALID;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean requireSigned = false;
        private boolean checkExpiry = true;
        private TrustStore trustStore;
        private Set<String> allowedDigestAlgorithms = Set.of("SHA-256", "SHA-384", "SHA-512");

        public Builder requireSigned(boolean requireSigned) {
            this.requireSigned = requireSigned;
            return this;
        }

        public Builder checkExpiry(boolean checkExpiry) {
            this.checkExpiry = checkExpiry;
            return this;
        }

        public Builder trustStore(TrustStore trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        public Builder allowedDigestAlgorithms(Set<String> algorithms) {
            this.allowedDigestAlgorithms = algorithms;
            return this;
        }

        public SignaturePolicy build() {
            return new SignaturePolicy(this);
        }
    }
}