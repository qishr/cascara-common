package io.github.qishr.cascara.test.common.security;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.ReportWriter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1ObjectIdentifier;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1PrintableString;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1UTCTime;
import io.github.qishr.cascara.common.security.cms.SignedData;

@Experimental
public class CMSUtils {
    public static void dump(Object obj, Reporter reporter) {
        dump(obj, reporter, 0);
    }

    public static void dump(Object obj, Reporter reporter, int depth) {
        int indent = depth * 2;
        ReportWriter writer = reporter.getWriter(Level.DEBUG);

        if (obj instanceof ASN1UTCTime t) {
            writer.write(indent, t);
        }
        else if (obj instanceof ASN1PrintableString s) {
            writer.write(indent, s);
        }
        else if (obj instanceof ASN1Sequence seq) {
            writer.write(indent, "SEQUENCE");

            // if (seq.size() > 3 && seq.get(2) instanceof ASN1Sequence oidSeq) {
            //     if (oidSeq.size() > 0 && oidSeq.get(0) instanceof ASN1ObjectIdentifier oid) {
            //         if (oid.getValue().startsWith("1.2.840.113549.1.7.")) {
            //             SignedData signed = SignedData.parse(seq);
            //             if (signed != null) {
            //                 reporter.info(GenericMessage.INFO, "Success!");
            //             }
            //         }
            //     }
            // }

            for (Object child : seq) {
                dump(child, reporter, depth + 1);
            }
        }
        else if (obj instanceof ASN1Set set) {
            writer.write(indent, "SET");

            for (Object child : set) {
                dump(child, reporter, depth + 1);
            }
        }
        else if (obj instanceof ASN1ObjectIdentifier oid) {
            writer.write(indent, "OID " + oid.getValue());
        }
        else if (obj instanceof ASN1TaggedObject tagged) {
            writer.write(indent, "TAGGED " + tagged.getTag());
            // writer.write(indent, "  getTag    : " + tagged.getTag());
            // writer.write(indent, "  getTagNo  : " + tagged.getTagNo());
            // writer.write(indent, "  isExplicit: " + tagged.isExplicit());
            dump(tagged.getObject(), reporter, depth + 1);
        }
        else {
            writer.write(indent, obj.getClass().getSimpleName());
        }
    }

    public static SignedData signedDataOrNull(ASN1Sequence seq) {
        // OID 1.2.840.113549
        if (seq.size() > 0 && seq.get(1) instanceof ASN1TaggedObject tagged) {
            if (tagged.getObject() instanceof ASN1Sequence certSeq) {
                SignedData signed = signedData(certSeq);
                return signed;
            }
        }
        return null;
    }

    private static SignedData signedData(ASN1Sequence seq) {
        if (seq.size() > 3 && seq.get(2) instanceof ASN1Sequence oidSeq) {
            if (oidSeq.size() > 0 && oidSeq.get(0) instanceof ASN1ObjectIdentifier oid) {
                if (oid.getValue().startsWith("1.2.840.113549.")) {
                    SignedData signed = SignedData.parse(seq);
                    return signed;
                }
            }
        }
        return null;
    }
}
