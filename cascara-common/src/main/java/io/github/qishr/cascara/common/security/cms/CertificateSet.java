package io.github.qishr.cascara.common.security.cms;

import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;

// CertificateSet ::= SET OF CertificateChoices

// CertificateChoices ::= CHOICE {
//     certificate Certificate,
//     extendedCertificate [0] IMPLICIT ExtendedCertificate,
//     v1AttrCert [1] IMPLICIT AttributeCertificateV1,
//     v2AttrCert [2] IMPLICIT AttributeCertificateV2,
//     other [3] IMPLICIT OtherCertificateFormat
// }

public final class CertificateSet {

    private final List<Certificate> certificates;

    private CertificateSet(List<Certificate> certificates) {
        this.certificates = certificates;
    }

    public List<Certificate> certificates() {
        return certificates;
    }

    public static CertificateSet parse(
            ASN1Sequence sequence) {

        List<Certificate> certificates =
                new ArrayList<>();

        for (Object child : sequence) {

            if (!(child instanceof ASN1Sequence cert)) {
                continue;
            }

            certificates.add(
                    Certificate.parse(cert)
            );
        }

        return new CertificateSet(certificates);
    }
}