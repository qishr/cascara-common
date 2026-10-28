package io.github.qishr.cascara.common.security;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class TrustStore {

    private final Set<X509Certificate> trustedCertificates;

    public TrustStore(Set<X509Certificate> trustedCertificates) {
        this.trustedCertificates = Collections.unmodifiableSet(new HashSet<>(trustedCertificates));
    }

    public static TrustStore empty() {
        return new TrustStore(Set.of());
    }

    /**
     * Loads a TrustStore from standard Cascara system properties:
     * -Dcascara.truststore=/path/to/truststore.jks
     * -Dcascara.truststore.password=secret
     */
    public static TrustStore fromSystemProperties() {
        String pathStr = System.getProperty("cascara.truststore");
        if (pathStr == null || pathStr.isBlank()) {
            return empty();
        }

        String passwordStr = System.getProperty("cascara.truststore.password", "");
        return load(Paths.get(pathStr), passwordStr.toCharArray());
    }

    public static TrustStore load(Path path, char[] password) {
        Set<X509Certificate> certs = new HashSet<>();
        try (InputStream is = Files.newInputStream(path)) {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(is, password);

            var aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isCertificateEntry(alias) && ks.getCertificate(alias) instanceof X509Certificate x509) {
                    certs.add(x509);
                }
            }
        } catch (Exception e) {
            throw new LocalizableSecurityException(e, SecurityMessage.NO_SUCH_ALGORITHM, path.toString());
        }
        return new TrustStore(certs);
    }

    public boolean contains(X509Certificate cert) {
        return trustedCertificates.contains(cert);
    }

    public boolean isTrusted(X509Certificate cert, boolean checkExpiry) {
        if (checkExpiry) {
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                return false;
            }
        }
        return trustedCertificates.contains(cert);
    }

    public Set<X509Certificate> getCertificates() {
        return trustedCertificates;
    }
}