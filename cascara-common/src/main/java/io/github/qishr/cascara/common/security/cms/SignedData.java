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
    private final static Reporter REPORTER = GlobalReporter.forClass(SignedData.class);

    private final ASN1Integer version;
    private final ASN1Set digestAlgorithms;
    private final ASN1Sequence contentInfo;
    private final ASN1TaggedObject certificates;
    private final ASN1Set signerInfos;

    // private final int version;
    // private final List<X509Certificate> certificates;

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

    public ASN1TaggedObject certificates() {
        return certificates;
    }

    // public int version() {
    //     return version;
    // }

    // public List<X509Certificate> certificates() {
    //     return certificates;
    // }

    public static SignedData parse(ASN1Sequence seq) {

        ASN1Integer version =
                (ASN1Integer) seq.get(0);

        ASN1Set digestAlgorithms =
                (ASN1Set) seq.get(1);

        ASN1Sequence contentInfo =
                (ASN1Sequence) seq.get(2);

        ASN1TaggedObject certificates =
                (ASN1TaggedObject) seq.get(3);

        ASN1Set signerInfos =
                (ASN1Set) seq.get(4);



        REPORTER.debug("Certificates:");
        CMSUtils.dump(certificates.getObject(), REPORTER);

        // CertificateSet.parse(
        //     (ASN1Sequence) certificates.getObject()
        // );

        return new SignedData(
                version,
                digestAlgorithms,
                contentInfo,
                certificates,
                signerInfos
        );
    }

    @Nullable
    public static SignedData _parse(ASN1Sequence sequence) throws CertificateException {

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

            // return new SignedData(
            //         version.intValue(),
            //         certificates
            // );
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
