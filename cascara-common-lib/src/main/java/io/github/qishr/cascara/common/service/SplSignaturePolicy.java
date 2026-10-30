package io.github.qishr.cascara.common.service;

import java.security.cert.Certificate;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.DataIgnore;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.SchemaDefinition;
import io.github.qishr.cascara.common.annotation.SchemaProperty;
import io.github.qishr.cascara.common.annotation.SchemaID;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.SignatureException;
import io.github.qishr.cascara.common.security.SignaturePolicy;
import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.security.cms.SignatureStatus;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarFile.JarEntry;

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

    private SplSignaturePolicy(Builder builder) {
        this.requireSigned = builder.requireSigned;
        this.checkExpiry = builder.checkExpiry;
        this.trustStore = builder.trustStore != null ? builder.trustStore : TrustStore.fromSystemProperties();
        this.allowedDigestAlgorithms = builder.allowedDigestAlgorithms;
    }

    public boolean getRequireSigned() {
        return requireSigned;
    }

    // TODO: Is this meant to be the same as the above?
    public boolean getTrustStoreEnforced() {
        return requireSigned;
    }

    public boolean getCheckExpiry() {
        return checkExpiry;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    // @Override
    // public SignatureStatus evaluate(JarFile jarFile) {
    //     try {
    //         JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
    //         if (manifestEntry == null) {
    //             return SignatureStatus.NOT_SIGNED;
    //         }

    //         Certificate[] certs = manifestEntry.getCertificates();
    //         if (certs == null || certs.length == 0) {
    //             return SignatureStatus.NOT_SIGNED;
    //         }

    //         // Evaluate certificates against trusted anchors...
    //         return SignatureStatus.VALID;
    //     } catch (SignatureException e) {
    //         REPORTER.error(e);
    //         return e.getSignatureStatus();
    //     }
    // }

    @Override
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

            // If no trust store is configured, any cryptographically valid signature passes
            if (trustStore == null) {
                return SignatureStatus.VALID;
            }

            // Check if any signing certificate is trusted by the TrustStore
            for (Certificate cert : certs) {
                if (cert instanceof X509Certificate x509 && isCertificateTrusted(x509)) {
                    return SignatureStatus.VALID;
                }
            }

            return SignatureStatus.UNTRUSTED_SIGNER;

        } catch (SignatureException e) {
            REPORTER.error(e);
            return e.getSignatureStatus();
        }
    }

    /**
     * Checks whether a certificate is directly trusted by the TrustStore or issued by a CA anchor in the TrustStore.
     */
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






    // The old evaluate method...
    @NonNull
    public SignatureStatus evaluateCert(X509Certificate cert) {
        if (cert == null) {
            return requireSigned ? SignatureStatus.NOT_SIGNED : SignatureStatus.VALID;
        }

        if (checkExpiry) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                return SignatureStatus.CERTIFICATE_EXPIRED;
            }
        }

        if (!trustStore.getCertificates().isEmpty() && !trustStore.contains(cert)) {
            return SignatureStatus.UNTRUSTED_SIGNER;
        }

        return SignatureStatus.VALID;
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

        public SplSignaturePolicy build() {
            return new SplSignaturePolicy(this);
        }
    }
}