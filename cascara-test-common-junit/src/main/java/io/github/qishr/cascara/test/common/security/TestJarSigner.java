package io.github.qishr.cascara.test.common.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.security.auth.x500.X500Principal;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Null;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1ObjectIdentifier;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1BitString;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1OctetString;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1UTCTime;
import io.github.qishr.cascara.test.common.jar.TestJarException;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public final class TestJarSigner {

    private static final Reporter REPORTER = Reporter.forClass(TestJarSigner.class);


    public record SignedJarBundle(
            byte[] manifestMf,
            byte[] manifestSf,
            byte[] key,
            X509Certificate certificate,
            PrivateKey privateKey,
            KeyAlgorithm algorithm
    ) {}

    private KeyPair keyPair;

    private TestSigningIdentity identity;
    private X509Certificate certificate;     // If null, uses identity.certificate()
    private PrivateKey privateKey;           // If null, uses identity.privateKey()
    private KeyAlgorithm signatureAlgorithm;       // Optional override
    // private boolean corruptSignatureBytes;  // Helpful for Layer 2 tests
    private boolean omitCertificates;        // For empty cert set test
    private String digestOidOverride;
    private List<X509Certificate> certificateChain = new ArrayList<>();


    public TestJarSigner withDigestOid(String digestOid) {
        this.digestOidOverride = digestOid;
        return this;
    }

    public TestJarSigner omitCertificates(boolean omit) {
        this.omitCertificates = omit;
        return this;
    }

    // public TestJarSigner withIdentity(TestSigningIdentity identity) {
    //     this.identity = identity;
    //     return this;
    // }

    public TestJarSigner withIdentity(TestSigningIdentity identity) {
        this.identity = identity;
        // Default to the identity's own cert if none explicitly provided
        if (this.certificateChain.isEmpty() && identity != null) {
            this.certificateChain.add(identity.certificate());
        }
        return this;
    }

    public TestJarSigner withCertificateChain(X509Certificate... certificates) {
        this.certificateChain = Arrays.asList(certificates);
        return this;
    }

    public TestJarSigner withCertificateChain(List<X509Certificate> certificates) {
        this.certificateChain = new ArrayList<>(certificates);
        return this;
    }

    public TestJarSigner withCertificate(X509Certificate cert) {
        this.certificate = cert;
        return this;
    }

    public TestJarSigner withPrivateKey(PrivateKey key) {
        this.privateKey = key;
        return this;
    }

    public TestJarSigner() {
        // Generate In-Memory RSA KeyPair & Self-Signed X.509 Certificate
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            keyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public TestJarSigner setKeyPair(KeyPair kp) {
        keyPair = kp;
        return this;
    }

    public X509Certificate generateSelfSignedCert(String dn) {
        return generateCert(dn, false);
    }

    public X509Certificate generateExpiredSelfSignedCert(String dn) {
        return generateCert(dn, true);
    }

    //
    //
    //

    // TODO: This needs to be updated to use the cert chain
    public SignedJarBundle sign(Map<String, byte[]> classFiles) {
        X509Certificate cert = this.certificate != null ? this.certificate
                : (identity != null ? identity.certificate() : generateSelfSignedCert("CN=Cascara Test Signer, OU=cascara, O=qishr, C=UK"));
        PrivateKey key = this.privateKey != null ? this.privateKey
                : (identity != null ? identity.privateKey() : keyPair.getPrivate());

        return sign(classFiles, cert, key);
    }

    public SignedJarBundle sign(Map<String, byte[]> classFiles, X509Certificate cert) {
        return sign(classFiles, cert, keyPair.getPrivate());
    }

    //
    //
    //

    // public SignedJarBundle sign(Map<String, byte[]> classFiles, X509Certificate cert, TestSigningIdentity signingId) throws Exception {
    private SignedJarBundle sign(Map<String, byte[]> classFiles, X509Certificate cert, PrivateKey privateKey) {
        REPORTER.debug("Signning with private key algorithm: " + privateKey.getAlgorithm());

        // 2. Build MANIFEST.MF with SHA-256 digests for each class file
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(new Attributes.Name("Created-By"), "Cascara Test Harness");

        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new TestJarException(e, e.getMessage());
        }

        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            Attributes attrs = new Attributes();
            byte[] digest = sha256.digest(entry.getValue());
            attrs.putValue("SHA-256-Digest", Base64.getEncoder().encodeToString(digest));
            mf.getEntries().put(entry.getKey(), attrs);
        }

        ByteArrayOutputStream mfBaos = new ByteArrayOutputStream();
        try {
            mf.write(mfBaos);
        } catch (IOException e) {
            throw new TestJarException(e, e.getMessage());
        }
        byte[] mfBytes = mfBaos.toByteArray();

        // 3. Build MANIFEST.SF with whole-manifest and entry digests
        Manifest sf = new Manifest();
        sf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        sf.getMainAttributes().put(new Attributes.Name("Created-By"), "Cascara Test Harness");

        byte[] mfDigest = sha256.digest(mfBytes);
        sf.getMainAttributes().putValue("SHA-256-Digest-Manifest", Base64.getEncoder().encodeToString(mfDigest));

        for (Map.Entry<String, Attributes> entry : mf.getEntries().entrySet()) {
            Attributes sfAttrs = new Attributes();
            String mfDigestValue = entry.getValue().getValue("SHA-256-Digest");
            byte[] entryDigest = sha256.digest(mfDigestValue.getBytes(StandardCharsets.UTF_8));
            sfAttrs.putValue("SHA-256-Digest", Base64.getEncoder().encodeToString(entryDigest));
            sf.getEntries().put(entry.getKey(), sfAttrs);
        }

        ByteArrayOutputStream sfBaos = new ByteArrayOutputStream();
        try {
            sf.write(sfBaos);
        } catch (IOException e) {
            throw new TestJarException(e, e.getMessage());
        }
        byte[] sfBytes = sfBaos.toByteArray();

        REPORTER.debug("  Private key: " + privateKey.getAlgorithm());

        // 4. Compute Digital Signature over MANIFEST.SF
        String algName = KeyAlgorithm.of(privateKey.getAlgorithm()).getSignatureAlgorithm();

        REPORTER.debug("  Signature: " + algName);

        try {
            Signature signature = Signature.getInstance(algName);
            signature.initSign(privateKey);
            signature.update(sfBytes);
            byte[] signatureBytes = signature.sign();

            // // 5. Build PKCS#7 SignedData (KEY.RSA) using Cascara ASN.1 objects
            // byte[] keyRsaBytes = buildPkcs7SignedData(cert, signatureBytes);
            // Resolve the certificate chain to embed (defaults to just the signing cert if no chain specified)
            List<X509Certificate> certChain = (this.certificateChain != null && !this.certificateChain.isEmpty())
                    ? this.certificateChain
                    : List.of(cert);

            // 5. Build PKCS#7 SignedData (KEY.RSA / KEY.EC) using Cascara ASN.1 objects
            byte[] keyRsaBytes = buildPkcs7SignedData(certChain, signatureBytes);


            // return new SignedJarBundle(mfBytes, sfBytes, keyRsaBytes, cert, privateKey, signingId == null ? null : signingId.algorithm());
            return new SignedJarBundle(mfBytes, sfBytes, keyRsaBytes, cert, privateKey, signatureAlgorithm);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new TestJarException(e, e.getMessage());
        }
    }

    // private byte[] buildPkcs7SignedData(X509Certificate cert, byte[] sigBytes) {
    private byte[] buildPkcs7SignedData(List<X509Certificate> certChain, byte[] sigBytes) {
        // The leaf / signing cert is the first in the chain
        X509Certificate signingCert = certChain.get(0);
        PublicKey publicKey = signingCert.getPublicKey();
        X509Certificate cert = certChain.get(0);


        DERDecoder decoder = new DERDecoder();
        // PublicKey publicKey = cert.getPublicKey();
        String keyAlg = publicKey.getAlgorithm().toUpperCase();

        REPORTER.debug("Creating PKCS7");
        REPORTER.debug("  Key Alg: " + keyAlg);


        // Standard PKCS#7 ContentInfo OIDs
        ASN1ObjectIdentifier pkcs7SignedDataOid = new ASN1ObjectIdentifier("1.2.840.113549.1.7.2");
        ASN1ObjectIdentifier pkcs7DataOid       = new ASN1ObjectIdentifier("1.2.840.113549.1.7.1");

        // 1. Resolve Digest OID matching the key / signature
        String digestOidStr = this.digestOidOverride != null
            ? this.digestOidOverride
            : resolveDefaultDigestOid(publicKey);

        ASN1ObjectIdentifier digestOid = new ASN1ObjectIdentifier(digestOidStr);
        REPORTER.debug("  digestOidStr: " + digestOidStr);

        // DigestAlgorithms SET
        ASN1Sequence digestAlgSeq = new ASN1Sequence(List.of(digestOid, new ASN1Null()));
        ASN1Set digestAlgSet = new ASN1Set(Set.of(digestAlgSeq));

        // EncapsulatedContentInfo: SEQUENCE { OID (data) }
        ASN1Sequence encapsulatedContentInfo = new ASN1Sequence(List.of(pkcs7DataOid));




        // Certificates IMPLICIT SET

        // ASN1TaggedObject implicitCerts;
        // if (omitCertificates) {
        //     // Empty SET OF Certificate: [0] IMPLICIT SET {}
        //     ASN1Set emptyCertSet = new ASN1Set(Collections.emptySet());
        //     implicitCerts = new ASN1TaggedObject(ASN1Tag.contextSpecific(0), emptyCertSet, false);
        // } else {

        //     ASN1Sequence certSeq;
        //     try (ASN1InputStream ais = new ASN1InputStream(decoder, cert.getEncoded())) {
        //         certSeq = (ASN1Sequence) ais.readObject();
        //     } catch (IOException | CertificateEncodingException e) {
        //         throw new TestJarException(e, e.getMessage());
        //     }
        //     ASN1Set rawCertSet = new ASN1Set(Set.of(certSeq));
        //     implicitCerts = new ASN1TaggedObject(ASN1Tag.contextSpecific(0), rawCertSet, false);
        // }
        // Certificates IMPLICIT SET

        // if (omitCertificates) {
        //     // Empty SET OF Certificate: IMPLICIT SET {}
        //     ASN1Set emptyCertSet = new ASN1Set(Collections.emptySet());
        //     implicitCerts = new ASN1TaggedObject(ASN1Tag.contextSpecific(0), emptyCertSet, false);
        // } else {
        //     Set<ASN1Encodable> certSetElements = new LinkedHashSet<>();
        //     for (X509Certificate cert : certChain) {
        //         try (ASN1InputStream ais = new ASN1InputStream(decoder, cert.getEncoded())) {
        //             ASN1Sequence certSeq = (ASN1Sequence) ais.readObject();
        //             certSetElements.add(certSeq);
        //         } catch (IOException | CertificateEncodingException e) {
        //             throw new TestJarException(e, e.getMessage());
        //         }
        //     }

        //     ASN1Set rawCertSet = new ASN1Set(certSetElements);
        //     implicitCerts = new ASN1TaggedObject(ASN1Tag.contextSpecific(0), rawCertSet, false);
        // }

        // Certificates IMPLICIT SET
        ASN1TaggedObject implicitCerts;
        if (omitCertificates) {
            // Empty SET OF Certificate: IMPLICIT SET {}
            ASN1Set emptyCertSet = new ASN1Set(Collections.emptySet());
            implicitCerts = new ASN1TaggedObject(ASN1Tag.contextSpecific(0), emptyCertSet, false);
        } else {
            Set<ASN1Object<?>> certSetElements = new LinkedHashSet<>();
            for (X509Certificate c : certChain) {
                try (ASN1InputStream ais = new ASN1InputStream(decoder, c.getEncoded())) {
                    ASN1Sequence certSeq = (ASN1Sequence) ais.readObject();
                    certSetElements.add(certSeq);
                } catch (IOException | CertificateEncodingException e) {
                    throw new TestJarException(e, e.getMessage());
                }
            }
            ASN1Set rawCertSet = new ASN1Set(certSetElements);
            implicitCerts = new ASN1TaggedObject(ASN1Tag.contextSpecific(0), rawCertSet, false);
        }




        // IssuerAndSerialNumber: SEQUENCE { Issuer DN Sequence, SerialNumber }
        ASN1Sequence issuerSeq;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, cert.getIssuerX500Principal().getEncoded())) {
            issuerSeq = (ASN1Sequence) ais.readObject();
        } catch (IOException e) {
            throw new TestJarException(e, e.getMessage());
        }
        ASN1Integer serialInt = new ASN1Integer(cert.getSerialNumber());
        ASN1Sequence issuerAndSerial = new ASN1Sequence(List.of(issuerSeq, serialInt));

        // 2. Resolve DigestEncryptionAlgorithmIdentifier
        ASN1Sequence sigAlgSeq;
        if ("EC".equals(keyAlg) || "ECDSA".equals(keyAlg)) {
            // For ECDSA, standard CMS uses id-ecPublicKey (1.2.840.10045.2.1)
            // or the combined ecdsa-with-SHA* OID (e.g. 1.2.840.10045.4.3.2 for SHA256withECDSA)
            // Parameters MUST be omitted for ECDSA (no ASN1Null).
            String ecdsaOid = switch (digestOidStr) {
                case "2.16.840.1.101.3.4.2.2" -> "1.2.840.10045.4.3.3"; // SHA384withECDSA
                case "2.16.840.1.101.3.4.2.3" -> "1.2.840.10045.4.3.4"; // SHA512withECDSA
                default                       -> "1.2.840.10045.4.3.2"; // SHA256withECDSA
            };
            REPORTER.debug("  ecdsaOid: " + ecdsaOid);
            sigAlgSeq = new ASN1Sequence(List.of(new ASN1ObjectIdentifier(ecdsaOid)));
        } else if ("DSA".equals(keyAlg)) {
            // DSA with SHA-256 (2.16.840.1.101.3.4.3.2) or SHA-1 (1.2.840.10040.4.3)
            sigAlgSeq = new ASN1Sequence(List.of(new ASN1ObjectIdentifier("2.16.840.1.101.3.4.3.2")));
        } else {
            // RSA: rsaEncryption (1.2.840.113549.1.1.1) with NULL parameters
            sigAlgSeq = new ASN1Sequence(List.of(new ASN1ObjectIdentifier("1.2.840.113549.1.1.1"), new ASN1Null()));
        }

        ASN1OctetString sigOctets = new ASN1OctetString(sigBytes);

                // SignerInfo: SEQUENCE { version, issuerAndSerial, digestAlg, sigAlg, signatureOctets }
        ASN1Sequence signerInfoSeq = new ASN1Sequence(List.of(
                new ASN1Integer(1),
                issuerAndSerial,
                digestAlgSeq,
                sigAlgSeq,
                sigOctets
        ));
        ASN1Set signerInfosSet = new ASN1Set(Set.of(signerInfoSeq));

        // Inner SignedData Sequence
        ASN1Sequence signedDataSeq = new ASN1Sequence(List.of(
                new ASN1Integer(1),
                digestAlgSet,
                encapsulatedContentInfo,
                implicitCerts,
                signerInfosSet
        ));

        // Outer ContentInfo: SEQUENCE { OID (SignedData), EXPLICIT SignedData }
        ASN1TaggedObject outerContent = new ASN1TaggedObject(ASN1Tag.contextSpecific(0), signedDataSeq, true);
        ASN1Sequence outerContentInfo = new ASN1Sequence(List.of(pkcs7SignedDataOid, outerContent));

        return outerContentInfo.getEncoded();
    }

    private X509Certificate generateCert(String dn, boolean expired) {
        DERDecoder decoder = new DERDecoder();

        // 1. Version: EXPLICIT INTEGER (v3 = 2)
        ASN1TaggedObject version = new ASN1TaggedObject(
                ASN1Tag.contextSpecific(0),
                new ASN1Integer(2),
                true
        );

        // 2. Serial Number
        ASN1Integer serialNumber = new ASN1Integer(
                new BigInteger(64, new SecureRandom()).abs()
        );

        // 3. Signature AlgorithmIdentifier: SEQUENCE { OID (sha256WithRSAEncryption), NULL }
        ASN1ObjectIdentifier sha256WithRsaOid = new ASN1ObjectIdentifier("1.2.840.113549.1.1.11");
        ASN1Sequence sigAlgSeq = new ASN1Sequence(List.of(sha256WithRsaOid, new ASN1Null()));

        // 4. Issuer & Subject DN Sequence (parsed directly from X500Principal DER bytes)
        X500Principal principal = new X500Principal(dn);
        // ASN1Sequence nameSeq = (ASN1Sequence) new ASN1InputStream(decoder, principal.getEncoded()).readObject();
        ASN1Sequence nameSeq = null;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, principal.getEncoded())) {
            nameSeq = (ASN1Sequence) ais.readObject();
        } catch (IOException e) {
            throw new TestJarException(e, e.getMessage());
        }

        // 5. Validity: SEQUENCE { UTCTime (NotBefore), UTCTime (NotAfter) }
        ASN1UTCTime notBefore = new ASN1UTCTime("261025000000Z".getBytes(StandardCharsets.US_ASCII));
        ASN1UTCTime notAfter  = expired
                ? new ASN1UTCTime("241025000000Z".getBytes(StandardCharsets.US_ASCII))
                : new ASN1UTCTime("271025000000Z".getBytes(StandardCharsets.US_ASCII));
        ASN1Sequence validitySeq = new ASN1Sequence(List.of(notBefore, notAfter));

        // 6. SubjectPublicKeyInfo (parsed from KeyPair public key bytes)
        // ASN1Sequence spkiSeq = (ASN1Sequence) new ASN1InputStream(decoder, keyPair.getPublic().getEncoded()).readObject();
        ASN1Sequence spkiSeq = null;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, keyPair.getPublic().getEncoded())) {
            spkiSeq = (ASN1Sequence) ais.readObject();
        } catch (IOException e) {
            throw new TestJarException(e, e.getMessage());
        }

        // 7. Assemble TBSCertificate SEQUENCE
        ASN1Sequence tbsSeq = new ASN1Sequence(List.of(
                version,
                serialNumber,
                sigAlgSeq,
                nameSeq,        // Issuer
                validitySeq,
                nameSeq,        // Subject
                spkiSeq
        ));

        // 8. Sign encoded TBSCertificate bytes using RSA private key
        byte[] tbsBytes = tbsSeq.getEncoded();
        byte[] signatureBytes;
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(keyPair.getPrivate());
            sig.update(tbsBytes);
            signatureBytes = sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new TestJarException(e, e.getMessage());
        }

        // 9. Assemble outer Certificate SEQUENCE: { tbsCertificate, signatureAlgorithm, signatureValue }
        ASN1BitString sigBitString = new ASN1BitString(signatureBytes, 0);
        ASN1Sequence certSeq = new ASN1Sequence(List.of(
                tbsSeq,
                sigAlgSeq,
                sigBitString
        ));

        // 10. Instantiate standard JDK X509Certificate from DER bytes
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(
                    new ByteArrayInputStream(certSeq.getEncoded())
            );
        } catch (CertificateException e) {
            throw new TestJarException(e, e.getMessage());
        }

    }

    // Default to SHA-256, or inspect bit size if P-384 / P-521
    private String resolveDefaultDigestOid(PublicKey publicKey) {
        String digestOidStr = "2.16.840.1.101.3.4.2.1"; // SHA-256
        if (publicKey instanceof java.security.interfaces.ECKey ecKey) {
            int fieldSize = ecKey.getParams().getCurve().getField().getFieldSize();
            if (fieldSize > 384) {
                digestOidStr = "2.16.840.1.101.3.4.2.3"; // SHA-512
            } else if (fieldSize > 256) {
                digestOidStr = "2.16.840.1.101.3.4.2.2"; // SHA-384
            }
        }
        return digestOidStr;
    }



    // public static String resolveDigestAlgorithm(String oid) throws UnsupportedDigestException {
    //     return switch (oid) {
    //         case "1.3.14.3.2.26"           -> "SHA-1";
    //         case "2.16.840.1.101.3.4.2.4" -> "SHA-224";
    //         case "2.16.840.1.101.3.4.2.1" -> "SHA-256";
    //         case "2.16.840.1.101.3.4.2.2" -> "SHA-384";
    //         case "2.16.840.1.101.3.4.2.3" -> "SHA-512";
    //         default -> throw new UnsupportedDigestException(oid);
    //     };
    // }
}
