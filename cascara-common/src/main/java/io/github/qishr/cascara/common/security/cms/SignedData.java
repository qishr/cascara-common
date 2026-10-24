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

import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;

public final class SignedData {

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

    public static SignedData parse(ASN1Sequence sequence) throws CertificateException {

        ASN1Integer version = (ASN1Integer) sequence.get(0);

        List<X509Certificate> certificates = new ArrayList<>();

        for (int i = 0; i < sequence.size(); i++) {

            ASN1Object object = sequence.get(i);

            if (!(object instanceof ASN1TaggedObject tagged)) {
                continue;
            }

            // if (tagged.getTag().getTagNo() != 0) {
            //     continue;
            // }
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

    private static List<X509Certificate> parseCertificateSet(
            ASN1TaggedObject tagged) throws CertificateException {

        List<X509Certificate> result =
                new ArrayList<>();

        byte[] bytes = tagged.getValueBytes();

        try (ASN1InputStream in = new ASN1InputStream(new DERDecoder(),bytes)) {

            ASN1Set set =
                    in.readObject();

            CertificateFactory cf =
                    CertificateFactory.getInstance(
                            "X.509");

            for (Object item : set) {

                ASN1Object certificate =
                        (ASN1Object) item;

                // byte[] encoded = certificate.getEncoded();
                byte[] encoded = certificate.getValueBytes();

                X509Certificate x509 =
                        (X509Certificate)
                                cf.generateCertificate(
                                        new ByteArrayInputStream(
                                                encoded));

                result.add(x509);
            }
        } catch (CertificateException e) {
            // TODO
            System.err.println(HexFormat.of()
                .formatHex(Arrays.copyOf(bytes, 16)));
            e.printStackTrace();
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }

        return result;
    }
}

// public class SignedData {
//     // SignedData ::= SEQUENCE {
//     //     version INTEGER,
//     //     digestAlgorithms SET,
//     //     contentInfo ContentInfo,
//     //     certificates [0] IMPLICIT CertificateSet OPTIONAL,
//     //     crls [1] IMPLICIT ...
//     //     signerInfos SET
//     // }

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
