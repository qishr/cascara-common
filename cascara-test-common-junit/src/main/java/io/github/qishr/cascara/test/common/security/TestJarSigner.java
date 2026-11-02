package io.github.qishr.cascara.test.common.security;

// public class TestJarSigner {
//     public static SyntheticJar sign(SyntheticJar testJar) {
//         return testJar;
//     }
// }

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.security.auth.x500.X500Principal;

import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
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
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public final class TestJarSigner {

    public record SignedJarBundle(
            byte[] manifestMf,
            byte[] manifestSf,
            byte[] key,
            X509Certificate certificate,
            PrivateKey privateKey,
            KeyAlgorithm algorithm
    ) {}

    private KeyPair keyPair;

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

    public X509Certificate generateSelfSignedCert(String dn) throws Exception {
        return generateCert(dn, false);
    }

    public X509Certificate generateExpiredSelfSignedCert(String dn) throws Exception {
        return generateCert(dn, true);
    }

    //
    //
    //

    public SignedJarBundle sign(Map<String, byte[]> classFiles) throws Exception {
        X509Certificate cert = generateSelfSignedCert("CN=Cascara Test Signer, OU=cascara, O=qishr, C=UK");
        return sign(classFiles, cert);
    }

    public SignedJarBundle sign(Map<String, byte[]> classFiles, X509Certificate cert) throws Exception {
        return sign(classFiles, cert, null);
    }

    // TODO: Remove this?
    public SignedJarBundle sign(Map<String, byte[]> classFiles, TestCertificateAuthority ca) throws Exception {
        // X509Certificate cert = generateSelfSignedCert("CN=Cascara Test Signer, OU=cascara, O=qishr, C=UK");
        // return sign(classFiles, cert, ca);
        return null;
    }

    public SignedJarBundle sign(Map<String, byte[]> classFiles, TestSigningIdentity sa) throws Exception {
        return sign(classFiles, sa.certificate(), sa);
    }

    public SignedJarBundle sign(Map<String, byte[]> classFiles, X509Certificate cert, TestSigningIdentity signingId) throws Exception {

        // 2. Build MANIFEST.MF with SHA-256 digests for each class file
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(new Attributes.Name("Created-By"), "Cascara Test Harness");

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        for (Map.Entry<String, byte[]> entry : classFiles.entrySet()) {
            Attributes attrs = new Attributes();
            byte[] digest = sha256.digest(entry.getValue());
            attrs.putValue("SHA-256-Digest", Base64.getEncoder().encodeToString(digest));
            mf.getEntries().put(entry.getKey(), attrs);
        }

        ByteArrayOutputStream mfBaos = new ByteArrayOutputStream();
        mf.write(mfBaos);
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
        sf.write(sfBaos);
        byte[] sfBytes = sfBaos.toByteArray();

        PrivateKey privateKey = signingId == null
            ? keyPair.getPrivate()
            : signingId.privateKey();

        // 4. Compute Digital Signature over MANIFEST.SF
        // Signature signature = Signature.getInstance("SHA256withRSA");
        String algName = signingId == null
            ? KeyAlgorithm.RSA.getSignatureAlgorithm()
            : signingId.algorithm().getSignatureAlgorithm();

        Signature signature = Signature.getInstance(algName);
        signature.initSign(privateKey);
        signature.update(sfBytes);
        byte[] signatureBytes = signature.sign();

        // 5. Build PKCS#7 SignedData (KEY.RSA) using Cascara ASN.1 objects
        byte[] keyRsaBytes = buildPkcs7SignedData(cert, signatureBytes);


        return new SignedJarBundle(mfBytes, sfBytes, keyRsaBytes, cert, privateKey, signingId == null ? null : signingId.algorithm());
    }

    //
    //
    //

    private byte[] buildPkcs7SignedData(X509Certificate cert, byte[] sigBytes) throws Exception {
        DERDecoder decoder = new DERDecoder();

        // OIDs
        ASN1ObjectIdentifier pkcs7SignedDataOid = new ASN1ObjectIdentifier("1.2.840.113549.1.7.2");
        ASN1ObjectIdentifier pkcs7DataOid       = new ASN1ObjectIdentifier("1.2.840.113549.1.7.1");
        ASN1ObjectIdentifier sha256Oid          = new ASN1ObjectIdentifier("2.16.840.1.101.3.4.2.1");
        ASN1ObjectIdentifier rsaEncryptionOid   = new ASN1ObjectIdentifier("1.2.840.113549.1.1.1");

        // DigestAlgorithms SET
        ASN1Sequence digestAlgSeq = new ASN1Sequence(List.of(sha256Oid, new ASN1Null()));
        ASN1Set digestAlgSet = new ASN1Set(Set.of(digestAlgSeq));

        // EncapsulatedContentInfo: SEQUENCE { OID (data) }
        ASN1Sequence encapsulatedContentInfo = new ASN1Sequence(List.of(pkcs7DataOid));

        // Certificates IMPLICIT SET
        // ASN1Sequence certSeq = (ASN1Sequence) new ASN1InputStream(decoder, cert.getEncoded()).readObject();
        ASN1Sequence certSeq = null;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, cert.getEncoded())) {
            certSeq = (ASN1Sequence) ais.readObject();
        }
        ASN1Set rawCertSet = new ASN1Set(Set.of(certSeq));
        ASN1TaggedObject implicitCerts = new ASN1TaggedObject(ASN1Tag.contextSpecific(0), rawCertSet, false);

        // IssuerAndSerialNumber: SEQUENCE { Issuer DN Sequence, SerialNumber }
        // ASN1Sequence issuerSeq = (ASN1Sequence) new ASN1InputStream(decoder, cert.getIssuerX500Principal().getEncoded()).readObject();
        ASN1Sequence issuerSeq = null;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, cert.getIssuerX500Principal().getEncoded())) {
            issuerSeq = (ASN1Sequence) ais.readObject();
        }
        ASN1Integer serialInt = new ASN1Integer(cert.getSerialNumber());
        ASN1Sequence issuerAndSerial = new ASN1Sequence(List.of(issuerSeq, serialInt));

        // SignerInfo: SEQUENCE { version, issuerAndSerial, digestAlg, sigAlg, signatureOctets }
        ASN1Sequence sigAlgSeq = new ASN1Sequence(List.of(rsaEncryptionOid, new ASN1Null()));
        ASN1OctetString sigOctets = new ASN1OctetString(sigBytes);

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

    private X509Certificate generateCert(String dn, boolean expired) throws Exception {
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
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(tbsBytes);
        byte[] signatureBytes = sig.sign();

        // 9. Assemble outer Certificate SEQUENCE: { tbsCertificate, signatureAlgorithm, signatureValue }
        ASN1BitString sigBitString = new ASN1BitString(signatureBytes, 0);
        ASN1Sequence certSeq = new ASN1Sequence(List.of(
                tbsSeq,
                sigAlgSeq,
                sigBitString
        ));

        // 10. Instantiate standard JDK X509Certificate from DER bytes
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(certSeq.getEncoded())
        );
    }
}