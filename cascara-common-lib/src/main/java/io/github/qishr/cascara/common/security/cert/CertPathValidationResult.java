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

import java.security.cert.CertPath;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.i18n.LocalizableMessage;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;

@Experimental
public final class CertPathValidationResult {
    @NonNull private final SignatureStatus status;
    private final CertPath certPath;
    private final TrustAnchor trustAnchor;
    private final int failedIndex;
    private final X509Certificate failedCertificate;
    private final DiagnosticMessage message;
    private final Throwable cause;

    private CertPathValidationResult(
            @NonNull SignatureStatus status,
            CertPath certPath,
            TrustAnchor trustAnchor,
            int failedIndex,
            X509Certificate failedCertificate,
            DiagnosticMessage message,
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
            DiagnosticMessage message,
            Throwable cause) {
        return new CertPathValidationResult(
                status, null, null, failedIndex, failedCertificate, message, cause);
    }

    public static CertPathValidationResult invalid(
            @NonNull SignatureStatus status,
            DiagnosticMessage message,
            Throwable cause) {
        return invalid(status, -1, null, message, cause);
    }

    public static CertPathValidationResult invalid(
            @NonNull SignatureStatus status,
            LocalizableMessage message) {
        return invalid(status, -1, null, new DiagnosticMessage(message), null);
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
    public DiagnosticMessage getMessage() {
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