package io.github.qishr.cascara.common.security.cms;

import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1ObjectIdentifier;

public class CmsParser {
    public static ContentInfo parseContentInfo(ASN1Object root) {
        ASN1Sequence seq = (ASN1Sequence) root;

        ASN1ObjectIdentifier oid =
                (ASN1ObjectIdentifier) seq.get(0);

        ASN1TaggedObject tagged =
                (ASN1TaggedObject) seq.get(1);

        ASN1Object content =
                tagged.getObject(); // EXPLICIT

        return new ContentInfo(
                oid.getValue(),
                content
        );
    }
}
