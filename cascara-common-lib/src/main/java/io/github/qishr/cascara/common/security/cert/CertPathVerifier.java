package io.github.qishr.cascara.common.security.cert;

import io.github.qishr.cascara.common.security.TrustStore;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.*;
import java.util.stream.Collectors;

public final class CertPathVerifier {

    private CertPathVerifier() {}

    /**
     * Validates a certificate chain against trusted anchors in a TrustStore.
     */
    public static CertPathValidationResult validatePath(
            List<X509Certificate> certificates,
            TrustStore trustStore) {
        return validatePath(certificates, trustStore, Collections.emptySet());
    }

    /**
     * Validates a certificate chain against trusted anchors in a TrustStore, optionally checking CRLs.
     */
    public static CertPathValidationResult validatePath(
            List<X509Certificate> certificates,
            TrustStore trustStore,
            Collection<X509CRL> crls) {

        if (certificates == null || certificates.isEmpty()) {
            return CertPathValidationResult.invalid("Certificate chain is empty or null");
        }
        if (trustStore == null || trustStore.getCertificates().isEmpty()) {
            return CertPathValidationResult.invalid("TrustStore contains no trusted anchors");
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(certificates);

            Set<TrustAnchor> trustAnchors = trustStore.getCertificates().stream()
                    .map(cert -> new TrustAnchor(cert, null))
                    .collect(Collectors.toSet());

            PKIXParameters params = new PKIXParameters(trustAnchors);

            if (crls != null && !crls.isEmpty()) {
                params.setRevocationEnabled(true);
                CertStoreParameters csp = new CollectionCertStoreParameters(crls);
                CertStore certStore = CertStore.getInstance("Collection", csp);
                params.addCertStore(certStore);
            } else {
                params.setRevocationEnabled(false);
            }

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(certPath, params);

            return CertPathValidationResult.valid(certPath, result.getTrustAnchor());

        } catch (CertPathValidatorException e) {
            return CertPathValidationResult.invalid(
                    "CertPath validation failed at index " + e.getIndex() + ": " + e.getMessage(), e);
        } catch (CertificateException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            return CertPathValidationResult.invalid("CertPath validation error: " + e.getMessage(), e);
        }
    }
}