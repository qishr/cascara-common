package io.github.qishr.cascara.common.security.cms;

import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;

record ContentInfo(
    String contentTypeOid,
    ASN1Object content
) {
}