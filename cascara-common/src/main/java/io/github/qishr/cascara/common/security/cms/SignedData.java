package io.github.qishr.cascara.common.security.cms;

import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;

// SignedData ::= SEQUENCE {
//     version INTEGER,
//     digestAlgorithms SET,
//     contentInfo ContentInfo,
//     certificates [0] IMPLICIT CertificateSet OPTIONAL,
//     crls [1] IMPLICIT ...
//     signerInfos SET
// }

public final class SignedData {
    private final ASN1Integer version;
    private final ASN1Set digestAlgorithms;
    private final ASN1Sequence contentInfo;
    private final ASN1TaggedObject certificates;
    private final ASN1Set signerInfos;

    public SignedData(
        ASN1Integer version,
        ASN1Set digestAlgorithms,
        ASN1Sequence contentInfo,
        ASN1TaggedObject certificates,
        ASN1Set signerInfos
    ) {

        this.version = version;
        this.digestAlgorithms = digestAlgorithms;
        this.contentInfo = contentInfo;
        this.certificates = certificates;
        this.signerInfos = signerInfos;
    }

    public ASN1Integer version() {
        return version;
    }

    public ASN1Set digestAlgorithms() {
        return digestAlgorithms;
    }

    public ASN1Sequence contentInfo() {
        return contentInfo;
    }

    public ASN1TaggedObject certificates() {
        return certificates;
    }

    public ASN1Set signerInfos() {
        return signerInfos;
    }

    public static SignedData parse(ASN1Sequence seq) {
        ASN1Integer version = (ASN1Integer) seq.get(0);
        ASN1Set digestAlgorithms = (ASN1Set) seq.get(1);
        ASN1Sequence contentInfo = (ASN1Sequence) seq.get(2);
        ASN1TaggedObject certificates = (ASN1TaggedObject) seq.get(3);
        ASN1Set signerInfos = (ASN1Set) seq.get(4);
        return new SignedData(
                version,
                digestAlgorithms,
                contentInfo,
                certificates,
                signerInfos
        );
    }
}
