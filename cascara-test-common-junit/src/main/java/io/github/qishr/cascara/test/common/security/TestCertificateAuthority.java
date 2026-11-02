package io.github.qishr.cascara.test.common.security;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1TaggedObject;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Boolean;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Integer;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1Null;
import io.github.qishr.cascara.common.security.asn1.types.primitive.ASN1ObjectIdentifier;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1BitString;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1OctetString;
import io.github.qishr.cascara.common.security.asn1.types.string.ASN1UTCTime;
import io.github.qishr.cascara.common.security.cert.SignatureAlgorithms;

public final class TestCertificateAuthority {
    private static final Reporter REPORTER = Reporter.forClass(TestCertificateAuthority.class);

    public record TestSigningIdentity(
            String alias,
            X509Certificate certificate,
            PrivateKey privateKey,
            KeyAlgorithm algorithm
    ) {}

    private final TestCertificateAuthority rootCA;
    private final X500Principal caPrincipal;
    private final KeyPair caKeyPair;
    private final KeyAlgorithm algorithm;
    private final X509Certificate caCertificate;

    /// @param rootCA if this constructor is creating the Root CA, rootCA should be null.
    private TestCertificateAuthority(TestCertificateAuthority rootCA, String caName) throws Exception {
        this.algorithm = KeyAlgorithm.RSA;
        REPORTER.debug(
            "Creating " +(rootCA == null ? "root " :"") +
            "CA ("+debugAlg(this.algorithm)+")"
        );
        this.rootCA = rootCA == null
            ? this
            : rootCA;
        this.caPrincipal = new X500Principal("CN=" + caName + ", O=Cascara Test Authority, C=UK");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        this.caKeyPair = keyGen.generateKeyPair();
        this.caCertificate = createCertificate(caPrincipal, caPrincipal, caKeyPair.getPublic(), caKeyPair.getPrivate(), 3650, rootCA == null);
    }

    /// @param rootCA if this constructor is creating the Root CA, rootCA should be null.
    private TestCertificateAuthority(TestCertificateAuthority rootCA, String caName, KeyPair keyPair, X509Certificate subCert, KeyAlgorithm algorithm) throws Exception {
        REPORTER.debug(
            "Creating " +(rootCA == null ? "root " :"") +
            "CA (" + debugAlg(algorithm) + ")"
        );
        this.rootCA = rootCA == null
            ? this
            : rootCA;
        this.caPrincipal = new X500Principal("CN=" + caName + ", O=Cascara Test Authority, C=UK");
        this.caKeyPair = keyPair;
        this.algorithm = algorithm;
        this.caCertificate = subCert;
    }

    public static TestCertificateAuthority create(String commonName, KeyAlgorithm algorithm) throws Exception {
        KeyPair keyPair = algorithm.generateKeyPair();
        X500Principal principal = new X500Principal("CN=" + commonName);

        X509Certificate rootCert = createCertificate(
                principal,
                principal,
                keyPair.getPublic(),
                keyPair.getPrivate(),
                3650,
                true // isCA = true
        );

        return new TestCertificateAuthority(null, commonName, keyPair, rootCert, algorithm);
    }

    private String debugAlg(KeyAlgorithm alg) {
        return "k=" + alg.getKeyAlgorithm() +
               " s=" + alg.getSignatureAlgorithm();
    }

    // Issue an identity inheriting the CA's algorithm
    public TestSigningIdentity issueIdentity(String commonName) throws Exception {
        REPORTER.debug("Creating identity with default algorithm (" + debugAlg(this.algorithm) + ")");
        return issueIdentity(commonName, this.algorithm);
    }

    // Or issue an identity with a specific key type
    public TestSigningIdentity issueIdentity(String commonName, KeyAlgorithm identityAlgorithm) throws Exception {
        REPORTER.debug("Creating identity with specified algorithm (" + debugAlg(identityAlgorithm) + ")");
        KeyPair keyPair = identityAlgorithm.generateKeyPair();
        X500Principal subject = new X500Principal("CN=" + commonName);

        X509Certificate cert = createCertificate(
                this.caCertificate.getSubjectX500Principal(),
                subject,
                keyPair.getPublic(),
                this.caKeyPair.getPrivate(),
                365,
                false // isCA = false
        );

        return new TestSigningIdentity(subject.getName(), cert, keyPair.getPrivate(), identityAlgorithm);
    }

    //
    //
    //

    public static TestCertificateAuthority create() throws Exception {
        return new TestCertificateAuthority(null, "Cascara Root CA");
    }

    public static TestCertificateAuthority create(String name) throws Exception {
        return new TestCertificateAuthority(null, name);
    }

    public TestCertificateAuthority createSubordinateCA(String commonName) throws Exception {
        REPORTER.debug("Creating subordinate CA with default algorithm (" + debugAlg(algorithm) + ")");
        KeyPair subKeyPair = algorithm.generateKeyPair();

        // In the X.509 generation logic (whether BouncyCastle or Cascara ASN.1 / Java security builder):
        X500Principal subSubject = new X500Principal("CN=" + commonName);

        // Build intermediate certificate:
        // - Issuer: this.caCertificate.getSubjectX500Principal()
        // - Subject: subSubject
        // - Public Key: subKeyPair.getPublic()
        // - BasicConstraints: isCA = true, pathLen = 0 or unconstrained
        // - KeyUsage: digitalSignature, keyCertSign, cRLSign
        // - Signed by: this.privateKey (Parent CA private key)
        X509Certificate subCert = createCertificate(
            this.caCertificate.getSubjectX500Principal(),
            subSubject,
            subKeyPair.getPublic(),
            rootCA.caKeyPair.getPrivate(),
            90,
            true
        );

        return new TestCertificateAuthority(rootCA, subSubject.getName(), subKeyPair, subCert, KeyAlgorithm.RSA);
    }

    public X509Certificate rootCertificate() {
        return caCertificate;
    }

    // TODO: At the moment, certificate and root certificate is the same thing.
    // This will change when suborinate CAs are added.

    public X509Certificate certificate() {
        return caCertificate;
    }

    public PrivateKey rootPrivateKey() {
        return caKeyPair.getPrivate();
    }

    public TrustStore trustStore() {
        return new TrustStore(Set.of(caCertificate));
    }

    public TestSigningIdentity issueIdentity(String name, long validityDays, KeyAlgorithm algorithm) throws Exception {
        REPORTER.debug("Issuing identity with algorithm (" + debugAlg(algorithm) + ")");

        // TODO:
        // KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm.getKeyAlgorithm());
        keyGen.initialize(2048);
        KeyPair childKeyPair = keyGen.generateKeyPair();



        X500Principal childPrincipal = new X500Principal("CN=" + name + ", OU=cascara-module, O=qishr, C=UK");
        X509Certificate childCert = createCertificate(
                // caPrincipal,              // Issuer = CA Name
                this.caCertificate.getSubjectX500Principal(),
                childPrincipal,           // Subject = Module Identity
                childKeyPair.getPublic(), // Child Public Key
                caKeyPair.getPrivate(),   // Signed by CA Private Key
                validityDays,
                false
        );

        return new TestSigningIdentity(name, childCert, childKeyPair.getPrivate(), algorithm);
    }

    public TestSigningIdentity issueExpiredIdentity(String name) throws Exception {
        REPORTER.debug("Issuing expired identity with algorithm (RSA)");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair childKeyPair = keyGen.generateKeyPair();

        X500Principal childPrincipal = new X500Principal("CN=" + name + ", OU=cascara-module, O=qishr, C=UK");
        X509Certificate expiredCert = createCertificate(
                caPrincipal,
                childPrincipal,
                childKeyPair.getPublic(),
                caKeyPair.getPrivate(),
                -1, // Validity expired 1 day ago
                false
        );

        return new TestSigningIdentity(name, expiredCert, childKeyPair.getPrivate(), KeyAlgorithm.RSA);
    }

    public X509CRL generateCrl(Set<BigInteger> revokedSerialNumbers) throws Exception {
        REPORTER.debug("Creating CRL with algorithm (SHA256withRSA)");
        Instant now = Instant.now();
        Instant nextUpdate = now.plus(7, ChronoUnit.DAYS);

        DateTimeFormatter utcFormatter = DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);
        byte[] thisUpdateBytes = utcFormatter.format(now).getBytes(StandardCharsets.US_ASCII);
        byte[] nextUpdateBytes = utcFormatter.format(nextUpdate).getBytes(StandardCharsets.US_ASCII);

        // 1. Signature Algorithm: SHA256withRSA (1.2.840.113549.1.1.11)
        ASN1Sequence sigAlg = new ASN1Sequence(List.of(
                new ASN1ObjectIdentifier("1.2.840.113549.1.1.11"),
                new ASN1Null()
        ));

        // 2. Decode raw issuer Name into an ASN1Object (typically ASN1Sequence)
        byte[] issuerDer = this.caCertificate.getSubjectX500Principal().getEncoded();
        ASN1Object<?> issuerName;
        try (ASN1InputStream ais = new ASN1InputStream(new DERDecoder(), new ByteArrayInputStream(issuerDer))) {
            issuerName = ais.readObject();
        }

        // 3. Build Revoked Certificates Sequence
        List<ASN1Object<?>> revokedListEntries = new ArrayList<>();
        if (revokedSerialNumbers != null && !revokedSerialNumbers.isEmpty()) {
            for (BigInteger sn : revokedSerialNumbers) {
                ASN1Sequence revokedEntry = new ASN1Sequence(List.of(
                        new ASN1Integer(sn),
                        new ASN1UTCTime(thisUpdateBytes)
                ));
                revokedListEntries.add(revokedEntry);
            }
        }

        // 4. Build TBSCertList Sequence
        List<ASN1Object<?>> tbsItems = new ArrayList<>();
        tbsItems.add(new ASN1Integer(BigInteger.valueOf(1))); // v2 = 1
        tbsItems.add(sigAlg);
        tbsItems.add(issuerName);
        tbsItems.add(new ASN1UTCTime(thisUpdateBytes));
        tbsItems.add(new ASN1UTCTime(nextUpdateBytes));

        if (!revokedListEntries.isEmpty()) {
            tbsItems.add(new ASN1Sequence(revokedListEntries));
        }

        ASN1Sequence tbsCertList = new ASN1Sequence(tbsItems);
        byte[] tbsBytes = tbsCertList.getEncoded();

        // 5. Sign TBSCertList
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(this.caKeyPair.getPrivate());
        sig.update(tbsBytes);
        byte[] signatureBytes = sig.sign();

        // 6. Complete CertificateList Sequence
        ASN1Sequence certList = new ASN1Sequence(List.of(
                tbsCertList,
                sigAlg,
                new ASN1BitString(signatureBytes, 0)
        ));

        // 7. Parse to java.security.cert.X509CRL
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509CRL) cf.generateCRL(new ByteArrayInputStream(certList.getEncoded()));
    }

    //
    // Private Methods
    //

    private static X509Certificate createCertificate(
            X500Principal issuer,
            X500Principal subject,
            PublicKey publicKey,
            PrivateKey signingKey,
            long validityDays,
            boolean isCA
    ) throws Exception {
        DERDecoder decoder = new DERDecoder();

        // 1. Version v3 (INTEGER 2)
        ASN1TaggedObject version = new ASN1TaggedObject(
                ASN1Tag.contextSpecific(0),
                new ASN1Integer(2),
                true
        );

        // 2. Serial Number
        ASN1Integer serialNumber = new ASN1Integer(new BigInteger(64, new SecureRandom()).abs());

        // Dynamically determine the signature algorithm matching the key
        String sigAlgName = SignatureAlgorithms.getDefaultSignatureAlgorithm(publicKey);
        String sigAlgOid = SignatureAlgorithms.getSignatureOid(sigAlgName);

        REPORTER.debug("Creating cert");
        REPORTER.debug("  Issuer: " + issuer.getName());
        REPORTER.debug("  Subject: " + subject.getName());
        REPORTER.debug("  Public key: " + publicKey.getAlgorithm());
        REPORTER.debug("  Signing key: " + signingKey.getAlgorithm());
        REPORTER.debug("  Signnature algorithm: " + sigAlgName);

        // 3. Signature AlgorithmIdentifier in ASN.1
        ASN1ObjectIdentifier sigOid = new ASN1ObjectIdentifier(sigAlgOid);
        ASN1Sequence sigAlgSeq = new ASN1Sequence(List.of(sigOid, new ASN1Null()));

        // 4. Issuer & Subject Sequences
        ASN1Sequence issuerSeq = null;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, issuer.getEncoded())) {
            issuerSeq = (ASN1Sequence) ais.readObject();
        }
        ASN1Sequence subjectSeq = null;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, subject.getEncoded())) {
            subjectSeq = (ASN1Sequence) ais.readObject();
        }

        // 5. Validity Period
        Instant now = Instant.now();
        Instant notBeforeInst = validityDays < 0 ? now.minus(30, ChronoUnit.DAYS) : now.minus(1, ChronoUnit.DAYS);
        Instant notAfterInst  = validityDays < 0 ? now.minus(1, ChronoUnit.DAYS)  : now.plus(validityDays, ChronoUnit.DAYS);

        ASN1UTCTime notBefore = new ASN1UTCTime(formatUtcTime(notBeforeInst).getBytes(StandardCharsets.US_ASCII));
        ASN1UTCTime notAfter  = new ASN1UTCTime(formatUtcTime(notAfterInst).getBytes(StandardCharsets.US_ASCII));
        ASN1Sequence validitySeq = new ASN1Sequence(List.of(notBefore, notAfter));

        // 6. SubjectPublicKeyInfo
        ASN1Sequence spkiSeq = null;
        try (ASN1InputStream ais = new ASN1InputStream(decoder, publicKey.getEncoded())) {
            spkiSeq = (ASN1Sequence) ais.readObject();
        }

        // 7. Extensions: EXPLICIT Extensions OPTIONAL
        List<ASN1Object<?>> tbsItems = new ArrayList<>(List.of(
                version,
                serialNumber,
                sigAlgSeq,
                issuerSeq,
                validitySeq,
                subjectSeq,
                spkiSeq
        ));

        if (isCA) {
            // BasicConstraints value ::= SEQUENCE { cA BOOLEAN DEFAULT FALSE }
            // For a CA cert: SEQUENCE { BOOLEAN true }
            ASN1Sequence bcValue = new ASN1Sequence(List.of(new ASN1Boolean(true)));
            byte[] bcValueEncoded = bcValue.getEncoded();

            // Extension ::= SEQUENCE { extnID, critical BOOLEAN, extnValue OCTET STRING }
            ASN1Sequence basicConstraintsExtension = new ASN1Sequence(List.of(
                    new ASN1ObjectIdentifier("2.5.29.19"), // id-ce-basicConstraints
                    new ASN1Boolean(true),                 // critical = true
                    new ASN1OctetString(bcValueEncoded)    // wrapped DER bytes
            ));

            // Extensions ::= SEQUENCE OF Extension
            ASN1Sequence extensionsSeq = new ASN1Sequence(List.of(basicConstraintsExtension));

            // EXPLICIT Extensions
            ASN1TaggedObject taggedExtensions = new ASN1TaggedObject(
                    ASN1Tag.contextSpecific(3),
                    extensionsSeq,
                    true
            );

            tbsItems.add(taggedExtensions);
        }

        // Build TBSCertificate
        ASN1Sequence tbsSeq = new ASN1Sequence(tbsItems);

        // 8. Sign TBSCertificate using the resolved algorithm
        Signature sig = Signature.getInstance(sigAlgName);
        sig.initSign(signingKey);
        sig.update(tbsSeq.getEncoded());
        byte[] signatureBytes = sig.sign();

        // 9. Outer Certificate Sequence
        ASN1BitString sigBitString = new ASN1BitString(signatureBytes, 0);
        ASN1Sequence certSeq = new ASN1Sequence(List.of(tbsSeq, sigAlgSeq, sigBitString));

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certSeq.getEncoded()));
    }

    private static String formatUtcTime(Instant instant) {
        var zdt = instant.atZone(java.time.ZoneOffset.UTC);
        return String.format("%02d%02d%02d%02d%02d%02dZ",
                zdt.getYear() % 100,
                zdt.getMonthValue(),
                zdt.getDayOfMonth(),
                zdt.getHour(),
                zdt.getMinute(),
                zdt.getSecond());
    }
}