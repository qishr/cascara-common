package io.github.qishr.cascara.common.security;

import java.security.cert.Certificate;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.jar.JarEntry;

import io.github.qishr.cascara.common.annotation.DataIgnore;
import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.SchemaDefinition;
import io.github.qishr.cascara.common.annotation.SchemaProperty;
import io.github.qishr.cascara.common.annotation.SchemaID;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.util.JarFile;

@Experimental
@SchemaDefinition(title = "SPL Signature Policy")
public final class SplSignaturePolicy implements SignaturePolicy {
    private final static Reporter REPORTER = Reporter.forClass(SplSignaturePolicy.class);

    @SchemaID
    public static final String SCHEMA = "https://qishr.github.io/schemas/spl-signature-policy.json";

    @SchemaProperty(title = "", description = "")
    private final boolean requireSigned;

    @SchemaProperty(title = "", description = "")
    private final boolean checkExpiry;

    @SchemaProperty(title = "", description = "")
    private final Set<String> allowedDigestAlgorithms;

    @DataIgnore
    private final TrustStore trustStore;

    @DataIgnore
    private final X509CRL crl;

    private SplSignaturePolicy(Builder builder) {
        this.requireSigned = builder.requireSigned;
        this.checkExpiry = builder.checkExpiry;
        this.trustStore = builder.trustStore != null ? builder.trustStore : TrustStore.fromSystemProperties();
        this.allowedDigestAlgorithms = builder.allowedDigestAlgorithms;
        this.crl = builder.crl;
    }

    public boolean getRequireSigned() {
        return requireSigned;
    }

    public boolean getTrustStoreEnforced() {
        return trustStore != null;
    }

    public boolean getCheckExpiry() {
        return checkExpiry;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    @Override
    @NonNull
    public SignatureStatus evaluate(JarFile jarFile) {
        try {
            JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            if (manifestEntry == null) {
                return SignatureStatus.NOT_SIGNED;
            }

            Certificate[] certs = manifestEntry.getCertificates();
            if (certs == null || certs.length == 0) {
                return SignatureStatus.NOT_SIGNED;
            }

            // TODO: This is handled by CascaraJarVerifier
            // Do we really need it here?
            if (checkExpiry) {
                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate x509) {
                        try {
                            x509.checkValidity();
                        } catch (CertificateExpiredException e) {
                            return SignatureStatus.CERTIFICATE_EXPIRED;
                        } catch (CertificateNotYetValidException e) {
                            return SignatureStatus.NOT_YET_VALID;
                        }
                    }
                }
            }

            // If no trust store is configured, check revocation if CRL present, otherwise VALID
            if (trustStore == null) {
                for (Certificate cert : certs) {
                    if (cert instanceof X509Certificate x509 && crl != null && crl.isRevoked(x509)) {
                        return SignatureStatus.CERTIFICATE_REVOKED;
                    }
                }
                return SignatureStatus.VALID;
            }

            boolean anyRevoked = false;

            // Check signing certificates against TrustStore and CRL
            for (Certificate cert : certs) {
                if (cert instanceof X509Certificate x509 && isCertificateTrusted(x509)) {
                    if (crl != null && crl.isRevoked(x509)) {
                        anyRevoked = true;
                        continue;
                    }
                    return SignatureStatus.VALID;
                }
            }

            // If a certificate was trusted by the TrustStore but revoked by the CRL
            if (anyRevoked) {
                return SignatureStatus.CERTIFICATE_REVOKED;
            }

            return SignatureStatus.UNTRUSTED_SIGNER;

        } catch (SignatureException e) {
            REPORTER.error(e);
            return e.getSignatureStatus();
        }
    }

    /// Checks whether a certificate is directly trusted by the TrustStore or issued by a CA anchor in the TrustStore.
    private boolean isCertificateTrusted(X509Certificate cert) {
        if (trustStore == null) {
            return true;
        }

        // 1. Direct match check (validity + presence)
        if (trustStore.isTrusted(cert, true)) {
            return true;
        }

        // 2. Issuer signature check against CA root anchors in the TrustStore
        for (X509Certificate caCert : trustStore.getCertificates()) {
            try {
                cert.verify(caCert.getPublicKey());
                return true; // Signed by a trusted CA anchor
            } catch (Exception ignored) {
                // Not signed by this anchor, try next
            }
        }

        return false;
    }

    //
    // Builder
    //

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean requireSigned = false;
        private boolean checkExpiry = true;
        private TrustStore trustStore;
        private X509CRL crl;
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

        public Builder addCrl(X509CRL crl) {
            this.crl = crl;
            return this;
        }

        public SplSignaturePolicy build() {
            return new SplSignaturePolicy(this);
        }
    }
}