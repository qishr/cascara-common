package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import integration.test.util.SourceCode;
import io.github.qishr.cascara.common.security.SignatureStatus;
import io.github.qishr.cascara.common.security.SplSignaturePolicy;
import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public class PolicyInvalidTests extends JarTestBase implements SourceCode {
    @Test
    void test_invalid_untrustedSigner_signaturePolicy() throws Exception {
        // 1. Create a trusted CA and a foreign CA
        TestCertificateAuthority cascaraCA = TestCertificateAuthority.create("Cascara Root CA");
        TestCertificateAuthority foreignCA = TestCertificateAuthority.create("Foreign Root CA");

        // 2. Sign module with foreign identity
        SyntheticJar foreignSignedJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempDir().resolve("foreign-provider.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
            .signWith(foreignCA.issueIdentity("foreign-module"))
            .packageJar();

        // 3. Configure SignaturePolicy requiring trust anchors from Cascara CA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
            .requireSigned(true)
            .trustStore(cascaraCA.trustStore())
            .build();

        // 4. Opening the JAR under Cascara policy rejects the foreign signer
        try (JarFile jar = JarFile.open(foreignSignedJar.getPath())) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status); // Failure: status is INVALID_SIGNATURE
        }
    }

    @Test
    void test_invalid_untrustedSigner_signaturePolicy(@TempDir Path tempDir) throws Exception {
        // Sign with an untrusted external CA
        TestCertificateAuthority rogueCA = TestCertificateAuthority.create("Rogue Root CA");
        TestSigningIdentity rogueIdentity = rogueCA.issueIdentity("rogue-module");
        Path jarPath = tempDir.resolve("untrusted-signer.jar");

        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .signWith(rogueIdentity)
                .packageJar();

        // Evaluate against our genuine TrustStore containing only Cascara Root CA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .trustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status);
        }
    }

    @Test
    void test_invalid_emptyTrustStore_rejectsAllSigners(@TempDir Path tempDir) throws Exception {
        TestSigningIdentity validIdentity = rootCA.issueIdentity("legit-module");
        Path jarPath = tempDir.resolve("legit-empty-ts.jar");

        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .signWith(validIdentity)
                .packageJar();

        // Empty TrustStore should fail to trust even a valid Cascara identity
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .trustStore(new TrustStore(Collections.emptySet()))
                .build();

        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status);
        }
    }

    @Test
    void test_invalid_unsignedJar_failsRequiredPolicy(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("unsigned.jar");

        // Build plain JAR without calling .signWith(...)
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .packageJar();

        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .trustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.NOT_SIGNED, status);
        }
    }

}
