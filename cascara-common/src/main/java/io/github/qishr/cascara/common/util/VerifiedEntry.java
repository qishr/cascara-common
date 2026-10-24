package io.github.qishr.cascara.common.util;

import java.security.cert.Certificate;
import java.security.CodeSigner;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;

final class VerifiedEntry {

    private final Certificate[] certificates;
    private final CodeSigner[] codeSigners;

    public VerifiedEntry(List<? extends Certificate> chain) throws CertificateException {

        this.certificates = chain.toArray(Certificate[]::new);

        CertificateFactory factory =
                CertificateFactory.getInstance("X.509");

        List<? extends Certificate> certificates = List.copyOf(chain);
        CertPath certPath =
                factory.generateCertPath(
                        certificates
                );

        this.codeSigners = new CodeSigner[] {
                new CodeSigner(certPath, null)
        };
    }

    public Certificate[] getCertificates() {
        return certificates.clone();
    }

    public CodeSigner[] getCodeSigners() {
        return codeSigners.clone();
    }
}