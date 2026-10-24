package io.github.qishr.cascara.common.security.cms;

import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;

public final class ContentInfo {

    private final String contentType;
    private final ASN1Object<?> content;

    public ContentInfo(
            String contentType,
            ASN1Object<?> content) {

        this.contentType = contentType;
        this.content = content;
    }

    public String contentType() {
        return contentType;
    }

    public ASN1Object<?> content() {
        return content;
    }
}

// record ContentInfo(
//     String contentTypeOid,
//     ASN1Object content
// ) {
// }