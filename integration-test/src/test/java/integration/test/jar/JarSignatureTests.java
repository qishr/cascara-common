package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import integration.test.spl.SplTestBase;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.SignatureException;
import io.github.qishr.cascara.common.security.SignatureStatus;
import io.github.qishr.cascara.common.security.SplSignaturePolicy;
import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.security.asn1.ASN1InputStream;
import io.github.qishr.cascara.common.security.asn1.encodingrules.ASN1Decoder;
import io.github.qishr.cascara.common.security.asn1.encodingrules.der.DERDecoder;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Object;
import io.github.qishr.cascara.common.security.asn1.types.ASN1Tag;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Sequence;
import io.github.qishr.cascara.common.security.asn1.types.constructed.ASN1Set;
import io.github.qishr.cascara.common.security.cms.CMSUtils;
import io.github.qishr.cascara.common.security.cms.CertificateSet;
import io.github.qishr.cascara.common.security.cms.SignedData;
import io.github.qishr.cascara.common.security.cms.SignerInfo;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarFile.JarEntry;
import io.github.qishr.cascara.common.util.ZipFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.KeyAlgorithm;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public class JarSignatureTests extends SplTestBase {
    private final static Reporter REPORTER = Reporter.forClass(JarSignatureTests.class);

    @BeforeAll
    static void startUp() {
        GlobalReporter.globalInstance()
            .setLevel(Level.DEBUG)
            .getFormatOptions()
                .setIncludeTime(false)
                .setIncluedPid(false)
                .setIncluedThread(false)
                .setUseSimpleClassName(true);
    }

    @Test
    void test_verifySignature() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
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
    }

    @Test
    void test_valid_readJarEntries() {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureTests.class)
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
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureTests.class)
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
        assertThrows(LocalizableSecurityException.class, () -> {
            for (JarEntry entry : jar.getJarEntries()) {
                InputStream is = jar.getInputStream(entry.getName());
                try {
                    is.close();
                } catch (Exception e) {
                    reporter.error(e);
                }
            }
        });

        try {
            jar.close();
        } catch (Exception e) {}
    }

    @Test
    void test_invalid_modifiedClass_readJarEntries() throws LocalizableIOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureTests.class)
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
        assertThrows(SignatureException.class, () -> {
            List<JarEntry> entries = jar.getJarEntries();
            for (JarEntry entry : entries) {
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
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureTests.class)
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
        assertThrows(LocalizableSecurityException.class, () -> {
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
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureTests.class)
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
        assertThrows(LocalizableSecurityException.class, () -> {
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
    void test_ecSignedJar_evaluatesValid(@TempDir Path tempDir) throws Exception {
        // 1. Create an EC Root CA
        TestCertificateAuthority ecCA = TestCertificateAuthority.create("Cascara EC Root CA", KeyAlgorithm.EC);
        TestSigningIdentity ecIdentity = ecCA.issueIdentity("ec-provider-module");

        // 2. Build synthetic JAR signed with EC identity
        SyntheticJar ecSignedJar = SyntheticJar.forUseIn(JarSignatureTests.class)
                .setPath(tempDir.resolve("ec-provider.jar"))
                // .addDependencies("cascara-common")
                // .addSources(sourceCodeA())
                .addSources("code/ModuleInfoA.txt", "code/ProviderA.txt")
                .signWith(ecIdentity)
                .packageJar();

        // 3. Verify under SignaturePolicy requiring ecCA trust anchor
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .trustStore(new TrustStore(Set.of(ecCA.certificate())))
                .build();

        try (JarFile jar = JarFile.open(ecSignedJar.getPath())) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.VALID, status);
        }
    }
}