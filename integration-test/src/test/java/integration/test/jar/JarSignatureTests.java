package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.jupiter.api.Test;

import integration.test.security.CertTests;
import integration.test.spl.SplTestBase;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.SignatureException;
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

public class JarSignatureTests extends SplTestBase {
    private final static Reporter REPORTER = Reporter.forClass(JarSignatureTests.class);

    @Test
    void testSignSyntheticJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(getClass())
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources("code/ModuleInfoA.txt", "code/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            byte[] mf = jar.extractFile("META-INF/MANIFEST.MF");
            byte[] sf = jar.extractFile("META-INF/MANIFEST.SF");
            JarManifestVerifier jv = new JarManifestVerifier(mf, sf);
            assertTrue(jv.verifySignatureFileAgainstManifest());
            for (JarEntry jarEntry : jar.getJarEntries()) {
                // Certificate[] certs = jarEntry.getCertificates();
                try (InputStream is = jar.getInputStream(jarEntry)) {
                    assertTrue(jv.verifyEntryContent(jarEntry.getName(), is));
                }
            }
        }
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
    void test_valid_JarEntry_old() {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureTests.class)
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
                } catch (Exception e) {}
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

}