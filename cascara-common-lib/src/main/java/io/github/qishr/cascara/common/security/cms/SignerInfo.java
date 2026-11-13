package io.github.qishr.cascara.common.security.cms;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1ObjectIdentifier;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1OctetString;
import io.github.qishr.cascara.common.security.diagnostic.InvalidKeyException;
import io.github.qishr.cascara.common.security.diagnostic.UnsupportedAlgorithmException;
import io.github.qishr.cascara.common.security.diagnostic.UnsupportedDigestException;
import io.github.qishr.cascara.common.security.policy.SignatureException;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;

public final class SignerInfo {

    private final BigInteger version;
    private final IssuerAndSerialNumber sid;
    private final String digestAlgorithmOid;
    private final ASN1Set signedAttrs; // IMPLICIT OPTIONAL
    private final String signatureAlgorithmOid;
    private final byte[] signatureBytes;

    public SignerInfo(
            BigInteger version,
            IssuerAndSerialNumber sid,
            String digestAlgorithmOid,
            ASN1Set signedAttrs,
            String signatureAlgorithmOid,
            byte[] signatureBytes
    ) {
        this.version = version;
        this.sid = sid;
        this.digestAlgorithmOid = digestAlgorithmOid;
        this.signedAttrs = signedAttrs;
        this.signatureAlgorithmOid = signatureAlgorithmOid;
        this.signatureBytes = signatureBytes;
    }

    public BigInteger version() {
        return version;
    }

    public IssuerAndSerialNumber signerIdentifier() {
        return sid;
    }

    public String digestAlgorithmOid() {
        return digestAlgorithmOid;
    }

    public ASN1Set signedAttrs() {
        return signedAttrs;
    }

    public String signatureAlgorithmOid() {
        return signatureAlgorithmOid;
    }

    public byte[] signatureBytes() {
        return signatureBytes;
    }

    public static SignerInfo parse(ASN1Sequence seq) {
        int idx = 0;

        // 1. Version
        ASN1Integer versionInt = (ASN1Integer) seq.get(idx++);
        BigInteger ver = versionInt.getValue();

        // 2. SignerIdentifier (IssuerAndSerialNumber)
        ASN1Sequence sidSeq = (ASN1Sequence) seq.get(idx++);
        IssuerAndSerialNumber sid = IssuerAndSerialNumber.parse(sidSeq);

        // 3. Digest Algorithm
        ASN1Sequence digestAlgSeq = (ASN1Sequence) seq.get(idx++);
        ASN1ObjectIdentifier digestOid = (ASN1ObjectIdentifier) digestAlgSeq.get(0);

        // 4. Signed Attributes IMPLICIT (Optional)
        ASN1Set signedAttrs = null;
        ASN1Object<?> nextObj = seq.get(idx);
        if (nextObj instanceof ASN1TaggedObject tagged && tagged.getTagNo() == 0) {
            signedAttrs = tagged.getObject(ASN1Tag.SET);
            idx++;
        }

        // 5. Signature Algorithm
        ASN1Sequence sigAlgSeq = (ASN1Sequence) seq.get(idx++);
        ASN1ObjectIdentifier sigAlgOid = (ASN1ObjectIdentifier) sigAlgSeq.get(0);

        // 6. Signature Bytes (ASN1OctetString)
        ASN1OctetString sigOctets = (ASN1OctetString) seq.get(idx);
        byte[] sigBytes = sigOctets.getValueBytes();

        return new SignerInfo(ver, sid, digestOid.getValue(), signedAttrs, sigAlgOid.getValue(), sigBytes);
    }

    /**
     * Finds the matching X509Certificate from a list of candidate certs.
     */
    public X509Certificate findSigningCertificate(Iterable<X509Certificate> certificates) {
        for (X509Certificate cert : certificates) {
            if (cert.getIssuerX500Principal().equals(sid.issuer())
                    && cert.getSerialNumber().equals(sid.serialNumber())) {
                return cert;
            }
        }
        return null;
    }

    /**
     * Verifies the signature bytes against the raw bytes of the .SF (Signature File).
     */
    public boolean verifySignature(X509Certificate cert, byte[] sfFileBytes) throws Exception {
        // Map OID to Standard Java Algorithm Name (e.g., "SHA256withRSA")

        String javaSigAlgorithm = resolveSignatureAlgorithm(digestAlgorithmOid, signatureAlgorithmOid);
        Signature sig;
        PublicKey publicKey;

        try {
            sig = Signature.getInstance(javaSigAlgorithm);
        } catch (java.security.NoSuchAlgorithmException e) {
            // TODO: Better status code
            // "No installed provider supports this key: sun.security.ec.ECPublicKeyImpl"
            throw new UnsupportedAlgorithmException(e, javaSigAlgorithm);
        } catch (Exception e) {
            throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Invalid signature: " + e.getMessage());
        }

        publicKey = cert.getPublicKey();

        try {
            sig.initVerify(publicKey);
        } catch (java.security.InvalidKeyException e) {
            // TODO: Better status code
            // "No installed provider supports this key: sun.security.ec.ECPublicKeyImpl"
            throw new InvalidKeyException(e, publicKey.getClass().getName());
        } catch (Exception e) {
            throw new SignatureException(e, SignatureStatus.INVALID_SIGNATURE, GenericMessage.ERROR, "Invalid signature: " + e.getMessage());
        }

        if (signedAttrs != null) {
            // A. Verify the MessageDigest attribute inside signedAttrs against hash(sfFileBytes)
            byte[] expectedDigest = extractMessageDigestAttribute(signedAttrs);
            if (expectedDigest != null) {
                String digestName = resolveDigestAlgorithm(digestAlgorithmOid);
                MessageDigest md = MessageDigest.getInstance(digestName);
                byte[] actualDigest = md.digest(sfFileBytes);

                if (!Arrays.equals(expectedDigest, actualDigest)) {
                    return false; // SF file digest mismatch!
                }
            }

            // B. Re-encode signedAttrs as universal SET OF (0x31) per RFC 5652 Section 5.3
            byte[] encodedSignedAttrs = signedAttrs.getEncoded();
            if (encodedSignedAttrs != null && encodedSignedAttrs.length > 0) {
                // Check if the leading tag byte at index 0 is context-specific (0xA0 / 0xA1)
                if ((encodedSignedAttrs[0] & 0xC0) == 0x80) {
                    encodedSignedAttrs[0] = (byte) 0x31; // Re-tag as universal SET OF (0x31) per RFC 5652
                }
            }

            sig.update(encodedSignedAttrs);
        } else {
            // Direct signature over raw .SF file bytes
            sig.update(sfFileBytes);
        }

        return sig.verify(signatureBytes);
    }

    private byte[] extractMessageDigestAttribute(ASN1Set attrs) {
        // OID 1.2.840.113549.1.9.4 = messageDigest attribute
        for (ASN1Object<?> obj : attrs) {
            if (obj instanceof ASN1Sequence seq && seq.size() >= 2) {
                if (seq.get(0) instanceof ASN1ObjectIdentifier oid && "1.2.840.113549.1.9.4".equals(oid.getValue())) {
                    if (seq.get(1) instanceof ASN1Set set) {
                        for (ASN1Object<?> setObj : set) {
                            if (setObj instanceof ASN1OctetString octets) {
                                return octets.getValueBytes();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String resolveDigestAlgorithm(String oid) {
        return switch (oid) {
            case "1.3.14.3.2.26" -> "SHA-1";
            case "2.16.840.1.101.3.4.2.1" -> "SHA-256";
            case "2.16.840.1.101.3.4.2.2" -> "SHA-384";
            case "2.16.840.1.101.3.4.2.3" -> "SHA-512";
            default -> "SHA-256";
        };
    }

    private static String resolveSignatureAlgorithm(String digestOid, String sigOid) {
        // Direct OID-to-Algorithm mappings (e.g. ecdsa-with-SHA256, sha256WithRSA, etc.)
        return switch (sigOid) {
            // ECDSA direct OIDs
            case "1.2.840.10045.4.1"   -> "SHA1withECDSA";
            case "1.2.840.10045.4.3.1" -> "SHA224withECDSA";
            case "1.2.840.10045.4.3.2" -> "SHA256withECDSA";
            case "1.2.840.10045.4.3.3" -> "SHA384withECDSA";
            case "1.2.840.10045.4.3.4" -> "SHA512withECDSA";

            // Generic EC public key (id-ecPublicKey) -> combine with digestOid
            case "1.2.840.10045.2.1" -> switch (digestOid) {
                case "1.3.14.3.2.26"           -> "SHA1withECDSA";
                case "2.16.840.1.101.3.4.2.4" -> "SHA224withECDSA";
                case "2.16.840.1.101.3.4.2.1" -> "SHA256withECDSA";
                case "2.16.840.1.101.3.4.2.2" -> "SHA384withECDSA";
                case "2.16.840.1.101.3.4.2.3" -> "SHA512withECDSA";
                // default                        -> "SHA256withECDSA";
                default -> throw new UnsupportedDigestException(digestOid);
            };

            // RSA Encryption (rsaEncryption) -> combine with digestOid
            case "1.2.840.113549.1.1.1", "1.2.840.113549.1.1.11", "1.2.840.113549.1.1.12" -> switch (digestOid) {
                case "1.3.14.3.2.26"           -> "SHA1withRSA";
                case "2.16.840.1.101.3.4.2.4" -> "SHA224withRSA";
                case "2.16.840.1.101.3.4.2.1" -> "SHA256withRSA";
                case "2.16.840.1.101.3.4.2.2" -> "SHA384withRSA";
                case "2.16.840.1.101.3.4.2.3" -> "SHA512withRSA";
                // default                        -> "SHA256withRSA";
                default -> throw new UnsupportedDigestException(digestOid);
            };

            // DSA
            case "1.2.840.10040.4.1", "1.2.840.10040.4.3" -> "SHA1withDSA";
            case "2.16.840.1.101.3.4.3.2"                  -> "SHA256withDSA";

            default -> {
                // If you created SignatureAlgorithms.java:
                // yield SignatureAlgorithms.getSignatureAlgorithmName(sigOid);

                // yield "SHA256withRSA";
                throw new UnsupportedDigestException(digestOid);
            }
        };
    }
}