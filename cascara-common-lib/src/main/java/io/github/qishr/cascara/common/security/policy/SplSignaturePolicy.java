package io.github.qishr.cascara.common.security.policy;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;

import io.github.qishr.cascara.common.annotation.DataIgnore;
import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.SchemaDefinition;
import io.github.qishr.cascara.common.annotation.SchemaProperty;
import io.github.qishr.cascara.common.annotation.SchemaID;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.cert.CertPathValidationResult;
import io.github.qishr.cascara.common.security.cert.CertPathVerifier;
import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.util.JarFile;

@Experimental
@SchemaDefinition(title = "SPL Signature Policy")
public final class SplSignaturePolicy implements SignaturePolicy {
    private final static Reporter REPORTER = Reporter.forClass(SplSignaturePolicy.class);

    @SchemaID
    public static final String SCHEMA = "https://qishr.github.io/schemas/spl-signature-policy.json";

    @SchemaProperty(title = "", description = "")
    private final boolean requireSigned;

    private final boolean requireSignedClasses;

    @SchemaProperty(title = "", description = "")
    private final boolean checkExpiry;

    @SchemaProperty(title = "", description = "")
    private final Set<String> allowedDigestAlgorithms;

    @DataIgnore
    private final TrustStore trustStore;

    private final boolean trustSystemModules;

    @DataIgnore
    private final X509CRL crl;

    private SplSignaturePolicy(Builder builder) {
        this.requireSigned = builder.requireSigned;
        this.requireSignedClasses = builder.requireSignedClasses;
        this.checkExpiry = builder.checkExpiry;
        this.trustStore = builder.trustStore != null ? builder.trustStore : TrustStore.fromSystemProperties();
        this.trustSystemModules = builder.trustSystemModules;
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
                if (requireSigned) {
                    return SignatureStatus.NOT_SIGNED;
                } else {
                    return SignatureStatus.VALID;
                }
            }

            Certificate[] certs = manifestEntry.getCertificates();
            if (certs == null || certs.length == 0) {
                if (requireSigned) {
                    return SignatureStatus.NOT_SIGNED;
                } else {
                    return SignatureStatus.VALID;
                }
            }

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

            List<X509Certificate> signerCertChain = Arrays.stream(certs)
                    .filter(c -> c instanceof X509Certificate)
                    .map(c -> (X509Certificate) c)
                    .toList();

            if (signerCertChain.isEmpty()) {
                return SignatureStatus.NOT_SIGNED;
            }

            // Evaluate the certificate chain extracted from PKCS#7 SignedData
            CertPathValidationResult vr = CertPathVerifier.validatePath(signerCertChain, this.trustStore); // Runs PKIX CertPathValidator

            if (!vr.isValid()) {
                // TODO: Reporter.debug to take DetailedMessage
                // All reporter methods should handle this
                // vr.gwetmessga should use detailde message
                // reporter debug and trace should use same formatting as error
                REPORTER.error(vr.getCause(), vr.getMessage());
                return vr.getStatus();
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

    @Override
    public SignatureStatus evaluate(Class<?> type) {
        if (!requireSignedClasses) {
            return SignatureStatus.VALID;
        }

        // Dynamic runtime proxies have no signed bytecode on disk
        if (Proxy.isProxyClass(type)) {
            return SignatureStatus.NOT_SIGNED;
        }

        // 1. System/Framework Exemption
        if (trustSystemModules && isSystemOrFrameworkClass(type)) {
            return SignatureStatus.VALID;
        }

        ProtectionDomain pd = type.getProtectionDomain();
        CodeSource cs = (pd != null) ? pd.getCodeSource() : null;

        if (cs == null || cs.getLocation() == null) {
            // Loose in-memory class, bootstrap class, or synthetic generated class
            if (requireSigned) {
                return SignatureStatus.NOT_SIGNED;
            } else {
                return SignatureStatus.VALID;
            }
        }

        // 1. If CodeSource certificates are already attached by the classloader:
        java.security.cert.Certificate[] certs = cs.getCertificates();
        if (certs != null && certs.length > 0) {
            // Evaluate the cert chain against policy
            List<X509Certificate> chain = Arrays.stream(certs)
                    .filter(c -> c instanceof X509Certificate)
                    .map(c -> (X509Certificate) c)
                    .toList();

            CertPathValidationResult vr = CertPathVerifier.validatePath(chain, trustStore);

            if (!vr.isValid()) {
                return vr.getStatus();
            }
        }

        // 2. If the classloader didn't pre-populate certs on CodeSource, inspect the location JAR:
        URL locationUrl = cs.getLocation();
        try {
            Path jarPath = Path.of(locationUrl.toURI());
            if (!Files.isRegularFile(jarPath)) {
                // Exploded directory on classpath, not a JAR
                if (requireSignedClasses) {
                    return SignatureStatus.NOT_SIGNED;
                } else {
                    return SignatureStatus.VALID;
                }
            }

            // Evaluate the JAR file using Cascara's JarFile & policy
            try (JarFile jar = JarFile.open(jarPath)) {
                SignatureStatus status = evaluate(jar);
                if (status != SignatureStatus.VALID) {
                    return status;
                }
            }
        } catch (URISyntaxException | IOException e) {
            if (requireSignedClasses) {
                return SignatureStatus.NOT_SIGNED;
            } else {
                return SignatureStatus.VALID;
            }
        }

        return SignatureStatus.VALID;
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

    private boolean isSystemOrFrameworkClass(Class<?> type) {
        if (type == null) {
            return false;
        }

        // 1. JDK Bootstrap / Core Classes (null ClassLoader)
        ClassLoader cl = type.getClassLoader();
        if (cl == null) {
            return true;
        }

        // 2. JDK Standard & Platform Modules (JPMS Check)
        Module module = type.getModule();
        if (module != null && module.isNamed()) {
            String moduleName = module.getName();
            if (moduleName.startsWith("java.") ||
                moduleName.startsWith("jdk.") ||
                moduleName.startsWith("sun.")) {
                return true;
            }
        }

        // 3. Cascara Trusted Core Framework Packages
        String pkgName = type.getPackageName();
        return isCascaraCorePackage(pkgName);
    }

    private boolean isCascaraCorePackage(String pkgName) {
        // Exempt foundational Cascara packages from extension signature checks
        return pkgName.startsWith("io.github.qishr.cascara.common") ||
               pkgName.startsWith("io.github.qishr.cascara.lang.json") ||
               pkgName.startsWith("io.github.qishr.cascara.schema");
    }

    //
    // Builder
    //

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean requireSigned = false;
        private boolean requireSignedClasses = false;
        private boolean checkExpiry = true;
        private TrustStore trustStore;
        private boolean trustSystemModules = true;
        private X509CRL crl;
        private Set<String> allowedDigestAlgorithms = Set.of("SHA-256", "SHA-384", "SHA-512");

        public Builder requireSigned(boolean requireSigned) {
            this.requireSigned = requireSigned;
            return this;
        }

        public Builder requireSignedClasses(boolean requireSignedClasses) {
            this.requireSignedClasses = requireSignedClasses;
            return this;
        }

        public Builder checkExpiry(boolean checkExpiry) {
            this.checkExpiry = checkExpiry;
            return this;
        }

        public Builder setTrustStore(TrustStore trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        public Builder trustSystemModules(boolean trustSystemModules) {
            this.trustSystemModules = trustSystemModules;
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