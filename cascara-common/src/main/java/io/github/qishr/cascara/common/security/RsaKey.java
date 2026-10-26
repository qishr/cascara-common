package io.github.qishr.cascara.common.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignedData;

public class RsaKey {
    List<X509Certificate> certificates;

    public RsaKey(InputStream in) {
        loadCerts(in);
    }

    public List<X509Certificate> getCertificates() {
        return certificates;
    }

    private void loadCerts(InputStream in) {
        ASN1Decoder dec = new DERDecoder();
        try (ASN1InputStream ais = new ASN1InputStream(dec, in)) {
            ASN1Sequence seq = ais.readObject();

            // A simple wrapper that checks the structure an calls SignedData.parse
            SignedData sd = CMSUtils.signedDataOrNull(seq);

            // assertNotNull(sd);

            // 1. Parse the tagged object as an implicit ASN1Set
            ASN1Set certSet = sd.certificates().getObject(ASN1Tag.SET);

            // 2. Parse the CertificateSet from the ASN1Set
            CertificateSet certificateSet = CertificateSet.parse(certSet);
            // assertNotNull(certificateSet);

            certificates = certificateSet.certificates()
                .stream()
                .map(c -> {
                    try {
                        return c.toX509Certificate();
                    } catch (CertificateException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return null;
                    }
                })
                .toList();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new SecurityException(e, GenericMessage.ERROR, e.getMessage());
        }
    }
}
