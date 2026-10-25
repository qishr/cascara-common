package io.github.qishr.cascara.common.security.cms;

import java.math.BigInteger;
import javax.security.auth.x500.X500Principal;

import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;

public final class IssuerAndSerialNumber {

    private final X500Principal issuer;
    private final BigInteger serialNumber;

    public IssuerAndSerialNumber(X500Principal issuer, BigInteger serialNumber) {
        this.issuer = issuer;
        this.serialNumber = serialNumber;
    }

    public X500Principal issuer() {
        return issuer;
    }

    public BigInteger serialNumber() {
        return serialNumber;
    }

    public static IssuerAndSerialNumber parse(ASN1Sequence seq) {
        // Index 0: Issuer Name (ASN1Sequence / RDNSequence)
        ASN1Sequence issuerSeq = (ASN1Sequence) seq.get(0);
        X500Principal principal = new X500Principal(issuerSeq.getEncoded());

        // Index 1: Serial Number
        ASN1Integer serialInt = (ASN1Integer) seq.get(1);

        return new IssuerAndSerialNumber(principal, serialInt.getValue());
    }
}