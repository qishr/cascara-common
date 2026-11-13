package io.github.qishr.cascara.common.security.cert;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.DetailedMessage;
import io.github.qishr.cascara.common.security.diagnostic.SecurityMessage;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;

public final class CertPathVerifier {

    private CertPathVerifier() {}

    /// Validates a certificate chain against trusted anchors in a TrustStore.
    public static CertPathValidationResult validatePath(
            List<X509Certificate> certificates,
            TrustStore trustStore) {
        return validatePath(certificates, trustStore, Collections.emptySet());
    }

    /// Validates a certificate chain against trusted anchors in a TrustStore, optionally checking CRLs.
    public static CertPathValidationResult validatePath(
            List<X509Certificate> certificates,
            TrustStore trustStore,
            Collection<X509CRL> crls) {

        if (certificates == null || certificates.isEmpty()) {
            return CertPathValidationResult.invalid(
                    SignatureStatus.NOT_SIGNED,
                    SecurityMessage.EMPTY_CERT_CHAIN
            );
        }
        if (trustStore == null || trustStore.getCertificates().isEmpty()) {
            return CertPathValidationResult.invalid(
                    SignatureStatus.UNTRUSTED_SIGNER,
                    SecurityMessage.EMPTY_TRUST_STORE
            );
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
            int failedIndex = e.getIndex();
            X509Certificate failedCert = (failedIndex >= 0 && failedIndex < certificates.size())
                    ? certificates.get(failedIndex)
                    : null;

            SignatureStatus status;
            if (e.getReason() == CertPathValidatorException.BasicReason.EXPIRED) {
                status = SignatureStatus.CERTIFICATE_EXPIRED;
            } else if (e.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                status = SignatureStatus.CERTIFICATE_REVOKED;
            } else {
                status = SignatureStatus.UNTRUSTED_SIGNER;
            }

            return CertPathValidationResult.invalid(
                    status,
                    failedIndex,
                    failedCert,
                    new DetailedMessage(
                        SecurityMessage.CERT_PATH_VALIDATION_FAILED,
                        failedCert == null
                            ? "Unknown"
                            : failedCert.getSubjectX500Principal().getName()
                    ),
                    e
            );
        } catch (CertificateException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            // TODO: Better error message
            return CertPathValidationResult.invalid(
                    SignatureStatus.INVALID_SIGNATURE,
                    DetailedMessage.forException(e),
                    e
            );
        }
    }
}


// public final class CertPathVerifier {

//     private CertPathVerifier() {}

//     /// Validates a certificate chain against trusted anchors in a TrustStore.
//     public static CertPathValidationResult validatePath(
//             List<X509Certificate> certificates,
//             TrustStore trustStore) {
//         return validatePath(certificates, trustStore, Collections.emptySet());
//     }

//     /// Validates a certificate chain against trusted anchors in a TrustStore, optionally checking CRLs.
//     public static CertPathValidationResult validatePath(
//             List<X509Certificate> certificates,
//             TrustStore trustStore,
//             Collection<X509CRL> crls) {

//         if (certificates == null || certificates.isEmpty()) {
//             return CertPathValidationResult.invalid("Certificate chain is empty or null");
//         }
//         if (trustStore == null || trustStore.getCertificates().isEmpty()) {
//             return CertPathValidationResult.invalid("TrustStore contains no trusted anchors");
//         }

//         try {
//             CertificateFactory cf = CertificateFactory.getInstance("X.509");
//             CertPath certPath = cf.generateCertPath(certificates);

//             Set<TrustAnchor> trustAnchors = trustStore.getCertificates().stream()
//                     .map(cert -> new TrustAnchor(cert, null))
//                     .collect(Collectors.toSet());

//             PKIXParameters params = new PKIXParameters(trustAnchors);

//             if (crls != null && !crls.isEmpty()) {
//                 params.setRevocationEnabled(true);
//                 CertStoreParameters csp = new CollectionCertStoreParameters(crls);
//                 CertStore certStore = CertStore.getInstance("Collection", csp);
//                 params.addCertStore(certStore);
//             } else {
//                 params.setRevocationEnabled(false);
//             }

//             CertPathValidator validator = CertPathValidator.getInstance("PKIX");
//             PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(certPath, params);

//             return CertPathValidationResult.valid(certPath, result.getTrustAnchor());

//         } catch (CertPathValidatorException e) {
//             return CertPathValidationResult.invalid(
//                     "CertPath validation failed at index " + e.getIndex() + ": " + e.getMessage(), e);
//         } catch (CertificateException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
//             return CertPathValidationResult.invalid("CertPath validation error: " + e.getMessage(), e);
//         }
//     }
// }