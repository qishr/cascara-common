package io.github.qishr.cascara.common.security.cms;

import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;

public class SignedData {
    // SignedData ::= SEQUENCE {
    //     version INTEGER,
    //     digestAlgorithms SET,
    //     contentInfo ContentInfo,
    //     certificates [0] IMPLICIT CertificateSet OPTIONAL,
    //     crls [1] IMPLICIT ...
    //     signerInfos SET
    // }

    public static SignedData parseSignedData(ASN1Object object) {
        for (ASN1Object child : sequence) {

            if (child instanceof ASN1TaggedObject tagged) {

                switch (tagged.getTag().getTagNo()) {

                    case 0:
                        certificates =
                                parseCertificateSet(tagged);
                        break;

                    case 1:
                        ...
                }
            }
        }
    }
}
