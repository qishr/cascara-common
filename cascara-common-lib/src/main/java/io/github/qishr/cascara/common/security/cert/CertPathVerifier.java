// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

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

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.security.SecurityMessage;
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
                    new DiagnosticMessage(
                        e,
                        SecurityMessage.CERT_PATH_VALIDATION_FAILED,
                        failedCert == null
                            ? "Unknown"
                            : failedCert.getSubjectX500Principal().getName()
                    ),
                    e
            );
        } catch (CertificateException | InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            return CertPathValidationResult.invalid(
                    SignatureStatus.INVALID_SIGNATURE,
                    DiagnosticMessage.forException(e),
                    e
            );
        }
    }
}
