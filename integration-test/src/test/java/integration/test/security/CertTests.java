package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import integration.test.util.SourceCode;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.SecurityException;
import io.github.qishr.cascara.common.security.SignaturePolicy;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignatureStatus;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarFile.CascaraJarVerifier;
import io.github.qishr.cascara.common.util.JarFile.JarEntry;
import io.github.qishr.cascara.common.util.ZipFile;
import io.github.qishr.cascara.test.common.junit.util.SyntheticJar;
import io.github.qishr.cascara.test.common.junit.util.TestBase;
import io.github.qishr.cascara.test.common.junit.util.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.junit.util.TestJarSigner;

public class CertTests extends TestBase implements SourceCode {
    private final static Reporter REPORTER = GlobalReporter.forClass(CertTests.class);

    @BeforeAll
    static void init() {
        GlobalReporter.globalInstance()
            .setLevel(Level.DEBUG)
            .getFormatOptions()
                .setIncludeTime(false)
                .setIncluedPid(false)
                .setIncluedThread(false)
                .setUseSimpleClassName(true);
    }

    @AfterAll
    static void end() {
        GlobalReporter.globalInstance()
            .setLevel(Level.INFO);
    }

    @Test
    void test_verifySignature() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        JarFile jar = JarFile.open(testJar.getPath());

        ASN1Decoder dec = new DERDecoder();
        try (InputStream in = jar.getInputStream("META-INF/TEST.RSA")) {
            try (ASN1InputStream ais = new ASN1InputStream(dec, in)) {
                ASN1Sequence seq = ais.readObject();

                // A simple wrapper that checks the structure an calls SignedData.parse
                SignedData sd = CMSUtils.signedDataOrNull(seq);

                assertNotNull(sd);

                // 1. Parse the tagged object as an implicit ASN1Set
                ASN1Set certSet = sd.certificates().getObject(ASN1Tag.SET);

                // 2. Parse the CertificateSet from the ASN1Set
                CertificateSet certificateSet = CertificateSet.parse(certSet);
                assertNotNull(certificateSet);

                List<X509Certificate> certificates = certificateSet.certificates()
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

                assertFalse(certificates.isEmpty());

                // 1. Parse SignerInfo set from SignedData
                ASN1Set signerInfoSet = sd.signerInfos();
                for (ASN1Object<?> obj : signerInfoSet) {
                    if (obj instanceof ASN1Sequence signerSeq) {
                        SignerInfo signerInfo = SignerInfo.parse(signerSeq);

                        // 2. Find matching certificate from our parsed CertificateSet
                        X509Certificate signingCert = signerInfo.findSigningCertificate(certificates);
                        assertNotNull(signingCert, "Signing certificate must be present in PKCS#7 block");

                        // 3. Read raw .SF file bytes from META-INF/
                        byte[] sfBytes = jar.extractFile("META-INF/TEST.SF");

                        // 4. Verify signature!
                        boolean isValid = signerInfo.verifySignature(signingCert, sfBytes);
                        assertTrue(isValid, "JAR Signature on MANIFEST.SF must be cryptographically valid!");

                        REPORTER.debug("Verified signature for: " + signingCert.getSubjectX500Principal());
                    }
                }
            }
        }
    }

    @Test
    void test_valid_JarEntry_old() {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {

            byte[] mfContent = jar.extractFile("META-INF/MANIFEST.MF");
            byte[] sfContent = jar.extractFile("META-INF/TEST.SF");

            JarManifestVerifier verifier = new JarManifestVerifier(mfContent, sfContent);
            boolean v1 = verifier.verifySignatureFileAgainstManifest();
            assertTrue(v1, "SF not verified against MF");

            String entryName = "com/example/providera/ProviderA.class";
            try (InputStream is = jar.getInputStream(entryName)) {
                boolean v2 = verifier.verifyEntryContent(entryName, is);
                assertTrue(v2, "Entry not verified");
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (LocalizableIOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void test_valid_readJarEntries() {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {

            for (JarEntry entry : jar.getJarEntries()) {
                InputStream is = jar.getInputStream(entry.getName());
                is.close();
            }

        } catch (LocalizableIOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void test_invalid_missingKey_readJarEntries() throws LocalizableIOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        // Remove the key file from the JAR
        try (ZipFile jar = ZipFile.open(testJar.getPath())) {
            jar.remove("META-INF/TEST.RSA");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Verify that an security exception is thrown
        JarFile jar = JarFile.open(testJar.getPath());
        assertThrows(SecurityException.class, () -> {
            for (JarEntry entry : jar.getJarEntries()) {
                InputStream is = jar.getInputStream(entry.getName());
                try {
                    is.close();
                } catch (Exception e) {}
            }
        });

        try {
            jar.close();
        } catch (Exception e) {}
    }

    @Test
    void test_invalid_modifiedClass_readJarEntries() throws LocalizableIOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        // Modify a class file in the JAR
        try (ZipFile jar = ZipFile.open(testJar.getPath())) {
            jar.addFile("garbage", "module-info.class");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Verify that an security exception is thrown
        JarFile jar = JarFile.open(testJar.getPath());
        assertThrows(SecurityException.class, () -> {
            for (JarEntry entry : jar.getJarEntries()) {
                InputStream is = jar.getInputStream(entry.getName());
                try {
                    is.close();
                } catch (Exception e) {}
            }
        });

        try {
            jar.close();
        } catch (Exception e) {}
    }

    @Test
    void test_invalid_tamperedSF_readJarEntries() throws LocalizableIOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        // Corrupt the .SF file in the JAR archive
        try (ZipFile jar = ZipFile.open(testJar.getPath())) {
            jar.addFile("SHA-256-Digest-Manifest: corruptedDigestValue==", "META-INF/TEST.SF");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Verify that a SecurityException is thrown due to .SF messageDigest mismatch in SignerInfo
        JarFile jar = JarFile.open(testJar.getPath());
        assertThrows(SecurityException.class, () -> {
            for (JarEntry entry : jar.getJarEntries()) {
                try (InputStream is = jar.getInputStream(entry.getName())) {
                    is.readAllBytes();
                }
            }
        });

        try {
            jar.close();
        } catch (Exception e) {}
    }

    @Test
    void test_invalid_missingSF_readJarEntries() throws LocalizableIOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        // Remove the .SF file while leaving TEST.RSA intact
        try (ZipFile jar = ZipFile.open(testJar.getPath())) {
            jar.remove("META-INF/TEST.SF");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Verify that a SecurityException is thrown when signature block lacks corresponding .SF
        JarFile jar = JarFile.open(testJar.getPath());
        assertThrows(SecurityException.class, () -> {
            for (JarEntry entry : jar.getJarEntries()) {
                try (InputStream is = jar.getInputStream(entry.getName())) {
                    is.readAllBytes();
                }
            }
        });

        try {
            jar.close();
        } catch (Exception e) {}
    }

    @Test
    void test_invalid_expiredCertificate() throws Exception {
        // 1. Generate an expired self-signed certificate (validity ended yesterday)
        TestJarSigner jarSigner = new TestJarSigner();
        KeyPair keyPair = jarSigner.getKeyPair();

        X509Certificate expiredCert = jarSigner.generateExpiredSelfSignedCert("CN=Expired Signer, O=cascara");

        // 2. Sign test jar bytes with the expired certificate
        Map<String, byte[]> classFiles = Map.of(
            "io/github/qishr/cascara/Foo.class", "class bytes".getBytes(StandardCharsets.UTF_8)
        );
        TestJarSigner.SignedJarBundle bundle = jarSigner.sign(classFiles, expiredCert);

        // 3. Verify checkValidity throws CertificateExpiredException
        assertThrows(CertificateExpiredException.class, expiredCert::checkValidity);

        // 4. Assert that SignerInfo or JarVerifier maps this to SignatureStatus.CERTIFICATE_EXPIRED
        DERDecoder decoder = new DERDecoder();
        ASN1Sequence rootSeq = (ASN1Sequence) new ASN1InputStream(decoder, bundle.keyRsa()).readObject();
        SignedData sd = CMSUtils.signedDataOrNull(rootSeq);
        SignerInfo signerInfo = SignerInfo.parse((ASN1Sequence) sd.signerInfos().iterator().next());

        assertThrows(CertificateExpiredException.class, () -> {
            X509Certificate cert = signerInfo.findSigningCertificate(List.of(expiredCert));
            cert.checkValidity(); // Enforced during trust path evaluation
        });
    }

    @Test
    void test_invalid_untrustedSigner_signaturePolicy() throws Exception {
        // 1. Create a trusted CA and a foreign CA
        TestCertificateAuthority cascaraCA = TestCertificateAuthority.create("Cascara Root CA");
        TestCertificateAuthority foreignCA = TestCertificateAuthority.create("Foreign Root CA");

        // 2. Sign module with foreign identity
        SyntheticJar foreignSignedJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("foreign-provider.jar"))
            .addDependencies("cascara-common", "integration-test-fixtures")
            .addSources(sourceCodeA())
            .signWith(foreignCA.issueIdentity("foreign-module"))
            .packageJar();

        // 3. Configure SignaturePolicy requiring trust anchors from Cascara CA
        SignaturePolicy policy = SignaturePolicy.builder()
            .requireSigned(true)
            .trustStore(cascaraCA.trustStore())
            .build();

        // 4. Opening the JAR under Cascara policy rejects the foreign signer
        JarFile jar = JarFile.open(foreignSignedJar.getPath());
        CascaraJarVerifier verifier = (CascaraJarVerifier) jar.getVerifier();

        SignatureStatus status = verifier.evaluatePolicy(policy);
        assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status);
        // TODO: decide how this is meant to work
        // how does policy fit inti spl?
        // should there be a default policy (set of options)
        // that can be overridden programatically andd through
        //n jvm env/props?

        // some things are checked before verifier is created.
        // is there a way to delay those results?

        // Should policy allow option of throwing exception
        // and/or setting flag to be checked by user?

        // typed exceptions and SignatureStatus in exceptions

        //
    }
}
