package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.i18n.exception.LocalizableSecurityException;
import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.security.exception.SignatureException;
import io.github.qishr.cascara.common.security.policy.SPLSignaturePolicy;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.ZipFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public class JarSignatureInvalidTests extends JarTestBase {
    private static final Reporter REPORTER = Reporter.forClass(JarSignatureValidTests.class);

    @Test
    void test_invalid_missingKey_readJarEntries() throws IOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempPath().resolve("provider.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
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
                    REPORTER.error(e);
                }
            }
        });

        try {
            jar.close();
        } catch (Exception e) {}
    }

    @Test
    void test_invalid_modifiedClass_readJarEntries() throws IOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempPath().resolve("provider.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
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
    void test_invalid_tamperedSF_readJarEntries() throws IOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempPath().resolve("provider.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
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
    void test_invalid_missingSF_readJarEntries() throws IOException {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempPath().resolve("provider.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
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
    void test_invalid_tamperedManifest_failsVerification() throws Exception {
        TestSigningIdentity identity = rootCA.issueIdentity("tamper-mf-module");
        Path jarPath = getTempPath().resolve("tampered-mf.jar");

        // 1. Create a signed JAR
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                // .signWith(identity)
                .sign(s -> s.withIdentity(identity))
                .packageJar();

        // 2. Tamper with META-INF/MANIFEST.MF directly inside the ZIP
        // (Changing an entry digest or appending extra characters breaks SHA-256-Digest-Manifest in .SF)
        tamperZipEntry(jarPath, "META-INF/MANIFEST.MF", original ->
                new String(original, StandardCharsets.UTF_8).replace("SHA-256-Digest:", "SHA-256-Digest: X").getBytes(StandardCharsets.UTF_8)
        );

        // 3. Verifying the JAR must throw SignatureException
        SignatureException ex = assertThrows(SignatureException.class, () -> {
            try (JarFile jar = JarFile.open(jarPath)) {
                // Extract a file, any file will cause the JAR's signature to be verified
                jar.extractFile("META-INF/MANIFEST.MF");
            }
        });
        assertEquals(SignatureStatus.INVALID_SIGNATURE, ex.getSignatureStatus());
    }

    @Test
    void test_invalid_unsignedEntryInjected_failsOnRead() throws Exception {
        TestSigningIdentity identity = rootCA.issueIdentity("injected-entry-module");
        Path jarPath = getTempPath().resolve("injected-entry.jar");

        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                // .signWith(identity)
                .sign(s -> s.withIdentity(identity))
                .packageJar();

        // Inject an unsigned file directly into the ZIP archive (not listed in MANIFEST.MF)
        addZipEntry(jarPath, "untracked/Malicious.class", new byte[]{0x01, 0x02, 0x03});

        try (JarFile jar = JarFile.open(jarPath)) {
            // Overall structure is valid, but reading the unauthorized entry must be rejected
            SignatureException ex = assertThrows(SignatureException.class, () ->
                    jar.extractFile("untracked/Malicious.class")
            );
            assertEquals(SignatureStatus.NOT_SIGNED, ex.getSignatureStatus());
        }
    }

    //
    // These tests target the internal integrity of the cryptographic signature block
    // itself (the `.RSA` / `.EC` file) and ensure `SignerInfo.verifySignature(...)`
    // and `CascaraJarVerifier` catch low-level cryptographic tampering.
    //

    @Test
    void test_invalid_corruptedSignatureBytes_failsVerification() throws Exception {
        TestSigningIdentity identity = rootCA.issueIdentity("corrupt-sig-bytes-module");
        Path jarPath = getTempPath().resolve("corrupt-sig.jar");

        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .sign(s -> s.withIdentity(identity))
                .packageJar();

        // Tamper with the raw signature block file (META-INF/*.RSA or *.EC)
        tamperZipEntry(jarPath, "META-INF/TEST.RSA", original -> {
            byte[] copy = original.clone();
            // Flip the last byte of the signature block
            copy[copy.length - 1] ^= 0xFF;
            return copy;
        });

        SignatureException ex = assertThrows(SignatureException.class, () -> {
            try (JarFile jar = JarFile.open(jarPath)) {
                // Extract a file, any file will cause the JAR's signature to be verified
                jar.extractFile("META-INF/MANIFEST.MF");
            }
        });
        assertEquals(SignatureStatus.INVALID_SIGNATURE, ex.getSignatureStatus());
    }

    @Test
    void test_invalid_mismatchedSignerCertificate_failsVerification() throws Exception {
        TestSigningIdentity signerA = rootCA.issueIdentity("signer-a");
        TestSigningIdentity signerB = rootCA.issueIdentity("signer-b");
        Path jarPath = getTempPath().resolve("mismatched-cert.jar");

        // Package the JAR where the SF is signed by signerA's private key,
        // but the PKCS#7 block packages signerB's certificate
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .sign(s -> s
                    .withPrivateKey(signerA.privateKey())
                    .withCertificate(signerB.certificate()))
                .packageJar();

        SignatureException ex = assertThrows(SignatureException.class, () -> {
            try (JarFile jar = JarFile.open(jarPath)) {
                // Extract a file, any file will cause the JAR's signature to be verified
                jar.extractFile("META-INF/MANIFEST.MF");
            }
        });
        assertEquals(SignatureStatus.INVALID_SIGNATURE, ex.getSignatureStatus());
    }

    @Test
    void test_invalid_tamperedSFWithSignedAttrs_failsVerification() throws Exception {
        TestSigningIdentity identity = rootCA.issueIdentity("signed-attrs-module");
        Path jarPath = getTempPath().resolve("tampered-sf-attrs.jar");

        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .sign(s -> s.withIdentity(identity)) // assuming signedAttrs are included or enabled
                .packageJar();

        // Alter the .SF file while keeping the PKCS#7 block intact
        tamperZipEntry(jarPath, "META-INF/TEST.SF", original -> {
            byte[] copy = original.clone();
            copy[copy.length - 1] ^= 0x01;
            return copy;
        });

        SignatureException ex = assertThrows(SignatureException.class, () -> {
            try (JarFile jar = JarFile.open(jarPath)) {
                // Extract a file, any file will cause the JAR's signature to be verified
                jar.extractFile("META-INF/MANIFEST.MF");
            }
        });
        assertEquals(SignatureStatus.INVALID_SIGNATURE, ex.getSignatureStatus());
    }

    @Test
    void test_invalid_missingSignerCertInBlock_failsVerification() throws Exception {
        TestSigningIdentity identity = rootCA.issueIdentity("missing-cert-module");
        Path jarPath = getTempPath().resolve("missing-cert-block.jar");

        // Package PKCS#7 block with an empty certificate set
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                // .customSign((sfBytes) -> buildPkcs7SignedDataWithEmptyCerts(identity, sfBytes))
                .sign(s -> s
                    .withIdentity(identity)
                    .omitCertificates(true))
                .packageJar();

        SignatureException ex = assertThrows(SignatureException.class, () -> {
            try (JarFile jar = JarFile.open(jarPath)) {
                // Extract a file, any file will cause the JAR's signature to be verified
                jar.extractFile("META-INF/MANIFEST.MF");
            }
        });
        assertEquals(SignatureStatus.INVALID_SIGNATURE, ex.getSignatureStatus());
    }

    //
    // Layer 3, we want to ensure that if a signer specifies an unknown digest algorithm OID in
    // `SignerInfo` (e.g. an arbitrary dummy OID or an unsupported obsolete hash), the verifier
    // detects it and aborts with `UnsupportedDigestException` / `SignatureStatus.UNSUPPORTED_ALGORITHM`.
    //

    @Test
    void test_invalid_unsupportedDigestOid_failsVerification() throws Exception {

        // GlobalReporter.globalInstance().setLevel("io.github.qishr.cascara.test.common.security.TestCertificateAuthority", Level.DEBUG);
        // GlobalReporter.globalInstance().setLevel("io.github.qishr.cascara.test.common.security.TestJarSigner", Level.DEBUG);

        TestSigningIdentity identity = rootCA.issueIdentity("unknown-digest-module");
        Path jarPath = getTempPath().resolve("unknown-digest.jar");

        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .sign(s -> s
                        .withIdentity(identity)
                        .withDigestOid("1.2.3.4.5.6.7.8.9")) // Non-existent / unknown digest OID
                .packageJar();

        SignatureException ex = assertThrows(SignatureException.class, () -> {
            try (JarFile jar = JarFile.open(jarPath)) {
                // Extract a file, any file will cause the JAR's signature to be verified
                jar.extractFile("META-INF/MANIFEST.MF");
            }
        });

        assertEquals(SignatureStatus.UNSUPPORTED_ALGORITHM, ex.getSignatureStatus());
    }

    @Test
    void test_invalid_intermediateNotCA_failsValidation() throws Exception {
        // 1. Root CA issues an intermediate that is NOT a CA (isCA = false)
        TestCertificateAuthority fakeSubCA = rootCA.issueUnauthorizedSubCA("unauthorized-sub-ca");

        // 2. That unauthorized intermediate signs a leaf identity
        TestSigningIdentity leafIdentity = fakeSubCA.issueIdentity("provider-module");

        // 3. Package and sign JAR with leaf + intermediate in the cert bundle
        Path jarPath = getTempPath().resolve("unauthorized-intermediate.jar");
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .sign(s -> s.withIdentity(leafIdentity)
                            .withCertificateChain(leafIdentity.certificate(), fakeSubCA.certificate()))
                .packageJar();

        // 4. Configure policy requiring valid signatures anchored at rootCA
        SPLSignaturePolicy policy = SPLSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        // 5. Evaluate policy
        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status);
        }
    }
}
