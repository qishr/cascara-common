package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import integration.test.jar.JarSignatureInvalidTests;
import integration.test.jar.JarSignatureValidTests;
import integration.test.jar.JarTestBase;
import io.github.qishr.cascara.common.security.policy.SplSignaturePolicy;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public class PolicyInvalidTests extends JarTestBase {

    @Test
    void test_invalid_foreignCA_signaturePolicy() throws Exception {
        // 1. Create a trusted CA and a foreign CA
        TestCertificateAuthority cascaraCA = TestCertificateAuthority.create("Cascara Root CA");
        TestCertificateAuthority foreignCA = TestCertificateAuthority.create("Foreign Root CA");

        TestSigningIdentity identity = foreignCA.issueIdentity("foreign-module");

        // 2. Sign module with foreign identity
        SyntheticJar foreignSignedJar = SyntheticJar.forUseIn(JarSignatureValidTests.class)
            .setPath(getTempPath().resolve("foreign-provider.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
            // .signWith(foreignCA.issueIdentity("foreign-module"))
            .sign(s -> s.withIdentity(identity))
            .packageJar();

        // 3. Configure SignaturePolicy requiring trust anchors from Cascara CA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
            .requireSigned(true)
            .setTrustStore(cascaraCA.trustStore())
            .build();

        // 4. Opening the JAR under Cascara policy rejects the foreign signer
        try (JarFile jar = JarFile.open(foreignSignedJar.getPath())) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status); // Failure: status is INVALID_SIGNATURE
        }
    }

    @Test
    void test_invalid_untrustedSigner_signaturePolicy() throws Exception {
        // Sign with an untrusted external CA
        TestCertificateAuthority rogueCA = TestCertificateAuthority.create("Rogue Root CA");
        TestSigningIdentity rogueIdentity = rogueCA.issueIdentity("rogue-module");
        Path jarPath = getTempPath().resolve("untrusted-signer.jar");

        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                // .signWith(rogueIdentity)
                .sign(s -> s.withIdentity(rogueIdentity))
                .packageJar();

        // Evaluate against our genuine TrustStore containing only Cascara Root CA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status);
        }
    }

    @Test
    void test_invalid_emptyTrustStore_rejectsAllSigners() throws Exception {
        TestSigningIdentity validIdentity = rootCA.issueIdentity("legit-module");
        Path jarPath = getTempPath().resolve("legit-empty-ts.jar");

        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                // .signWith(validIdentity)
                .sign(s -> s.withIdentity(validIdentity))
                .packageJar();

        // Empty TrustStore should fail to trust even a valid Cascara identity
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(new TrustStore(Collections.emptySet()))
                .build();

        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status);
        }
    }

    @Test
    void test_invalid_unsignedJar_failsRequiredPolicy() throws Exception {
        Path jarPath = getTempPath().resolve("unsigned.jar");

        // Build plain JAR without calling .signWith(...)
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .setPath(jarPath)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .packageJar();

        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.NOT_SIGNED, status);
        }
    }

    @Test
    void test_invalid_expiredCert_policyEvaluationFails() throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");
        Path jarPath = getTempPath().resolve("expired-cert.jar");

        // 2. Package and sign JAR
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .sign(s -> s.withIdentity(expiredIdentity))
                .packageJar(jarPath);

        // 3. Configure policy requiring valid signatures trusted by rootCA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        // 4. Policy evaluation must reject with CERTIFICATE_EXPIRED
        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.CERTIFICATE_EXPIRED, status);
        }
    }

    @Test
    void test_invalid_notYetValidCert_policyEvaluationFails() throws Exception {
        // 1. Issue an identity whose validity window starts in the future
        TestSigningIdentity futureIdentity = rootCA.issueFutureIdentity("future-module");
        Path jarPath = getTempPath().resolve("not-yet-valid-cert.jar");

        // 2. Package and sign JAR
        SyntheticJar.forUseIn(PolicyInvalidTests.class)
                .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
                .sign(s -> s.withIdentity(futureIdentity))
                .packageJar(jarPath);

        // 3. Configure policy requiring valid signatures trusted by rootCA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        // 4. Policy evaluation must reject with NOT_YET_VALID
        try (JarFile jar = JarFile.open(jarPath)) {
            SignatureStatus status = policy.evaluate(jar);
            assertEquals(SignatureStatus.NOT_YET_VALID, status);
        }
    }
}
