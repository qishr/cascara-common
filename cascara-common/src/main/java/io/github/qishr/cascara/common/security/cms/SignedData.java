package io.github.qishr.cascara.common.security.cms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
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
    private final static Reporter REPORTER = GlobalReporter.forClass(SignedData.class);

    private final int version;
    private final List<X509Certificate> certificates;

    public SignedData(
            int version,
            List<X509Certificate> certificates) {

        this.version = version;
        this.certificates = certificates;
    }

    public int version() {
        return version;
    }

    public List<X509Certificate> certificates() {
        return certificates;
    }

    @Nullable
    public static SignedData parse(ASN1Sequence sequence) throws CertificateException {

        ASN1Object<?> obj = sequence.get(0);
        if (obj instanceof ASN1Integer version) {

            List<X509Certificate> certificates = new ArrayList<>();

            for (int i = 0; i < sequence.size(); i++) {
                ASN1Object<?> object = sequence.get(i);
                if (!(object instanceof ASN1TaggedObject tagged)) {
                    continue;
                }
                if (tagged.getTagNo() != 0) {
                    continue;
                }
                certificates.addAll(
                        parseCertificateSet(tagged)
                );
            }

            return new SignedData(
                    version.intValue(),
                    certificates
            );
        }
        return null;
        // throw new CertificateException("Expected ASN1Integer but got: " + obj.getClass().getSimpleName());
    }

    private static List<X509Certificate> parseCertificateSet(ASN1TaggedObject tagged) throws CertificateException {
        List<X509Certificate> result = new ArrayList<>();
        byte[] bytes = tagged.getValueBytes();
        try (ASN1InputStream in = new ASN1InputStream(new DERDecoder(),bytes)) {
            ASN1Object<?> obj = in.readObject();
            if (obj instanceof ASN1Sequence set) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                for (ASN1Object<?> certificate : set) {
                    try {
                        //TODO: is getEncoded meant to construct the encode value?
                        // byte[] encoded = certificate.getEncoded();
                        byte[] encoded = certificate.getValueBytes();
                        X509Certificate x509 =
                                (X509Certificate)
                                        cf.generateCertificate(
                                                new ByteArrayInputStream(
                                                        encoded));
                        result.add(x509);
                    } catch (CertificateException e) {
                        // TODO: Throw the exception or just report it?
                        REPORTER.error(e,
                            GenericMessage.ERROR,
                            e.getMessage()
                        );
                        REPORTER.debug(
                            "Bytes: " +
                                HexFormat.of().formatHex(Arrays.copyOf(bytes, 16))
                        );
                    }
                }
            } else {
                REPORTER.warn(
                    GenericMessage.WARN,
                    "Unexpected: " + obj
                );
            }
        } catch (IOException e) {
            // TODO: Throw the exception or just report it?
            REPORTER.error(e,
                GenericMessage.ERROR,
                e.getMessage()
            );
        }

        return result;
    }
}


//     public static SignedData parseSignedData(ASN1Object object) {
//         for (ASN1Object child : sequence) {

//             if (child instanceof ASN1TaggedObject tagged) {

//                 switch (tagged.getTag().getTagNo()) {

//                     case 0:
//                         certificates =
//                                 parseCertificateSet(tagged);
//                         break;

//                     case 1:
//                         ...
//                 }
//             }
//         }
//     }
// }
