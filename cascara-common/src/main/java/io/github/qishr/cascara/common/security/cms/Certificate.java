package io.github.qishr.cascara.common.security.cms;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;

public final class Certificate {

    private final ASN1Sequence sequence;

    private Certificate(ASN1Sequence sequence) {
        this.sequence = sequence;
    }

    public static Certificate parse(ASN1Sequence sequence) {
        return new Certificate(sequence);
    }

    public ASN1Sequence sequence() {
        return sequence;
    }

    public X509Certificate toX509Certificate() throws CertificateException {
        byte[] encoded = sequence.getEncoded();
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate)
                factory.generateCertificate(
                        new ByteArrayInputStream(encoded));
    }
}