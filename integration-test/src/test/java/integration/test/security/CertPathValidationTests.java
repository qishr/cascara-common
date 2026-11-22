package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.security.cert.CertPathValidationResult;
import io.github.qishr.cascara.common.security.cert.CertPathVerifier;
import io.github.qishr.cascara.common.security.policy.SplSignaturePolicy;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.junit.base.TestBase;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

@Isolated
class CertPathValidationTests extends TestBase {
    private static final Reporter REPORTER = Reporter.forClass(CertPathValidationTests.class);

    private static TestCertificateAuthority rootCA;
    private static TestCertificateAuthority intermediateCA;
    private static TestCertificateAuthority untrustedCA;

    @BeforeAll
    static void init() throws Exception {
        rootCA = TestCertificateAuthority.create("Root CA");
        // Intermediate signed by Root:
        intermediateCA = rootCA.createSubordinateCA("Intermediate CA");
        // Completely unrelated CA:
        untrustedCA = TestCertificateAuthority.create("Untrusted Foreign CA");
    }

    @Test
    void test_validCertChain_trustedByRootAnchor() throws Exception {
        // End-entity signed by Intermediate CA
        TestSigningIdentity leaf = intermediateCA.issueIdentity("leaf-module");

        // Chain provided in PKCS#7 / JAR: [leaf, intermediate]
        List<X509Certificate> chain = List.of(leaf.certificate(), intermediateCA.certificate());
        TrustStore trustStore = new TrustStore(Set.of(rootCA.certificate()));

        CertPathValidationResult result = CertPathVerifier.validatePath(chain, trustStore);
        if (!result.isValid()) {
            // TODO: Check the message is correct
            REPORTER.error(result.getCause(), result.getMessage());
        }
        assertTrue(result.isValid());
    }

    @Test
    void test_brokenCertChain_missingIntermediate() throws Exception {
        TestSigningIdentity leaf = intermediateCA.issueIdentity("leaf-module");

        // Chain missing intermediate: only [leaf] provided
        List<X509Certificate> chain = List.of(leaf.certificate());
        TrustStore trustStore = new TrustStore(Set.of(rootCA.certificate()));

        CertPathValidationResult result = CertPathVerifier.validatePath(chain, trustStore);
        assertFalse(result.isValid());
        // Should indicate inability to build/find path to trust anchor
    }

    @Test
    void test_untrustedRootAnchor_failsValidation() throws Exception {
        TestSigningIdentity leaf = untrustedCA.issueIdentity("foreign-leaf");
        List<X509Certificate> chain = List.of(leaf.certificate());

        TrustStore trustStore = new TrustStore(Set.of(rootCA.certificate()));

        CertPathValidationResult result = CertPathVerifier.validatePath(chain, trustStore);
        assertFalse(result.isValid());
    }

    @Test
    void test_emptyTrustStore_rejectsAllSignatures() throws Exception {
        SyntheticJar jar = SyntheticJar.forUseIn(CertPathValidationTests.class)
            .setPath(getTempPath().resolve("expired-intermediate.jar"))
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        // Configured TrustStore is explicitly empty
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(TrustStore.empty())
                .build();

        try (JarFile jarFile = JarFile.open(jar.getPath())) {
            SignatureStatus status = policy.evaluate(jarFile);
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status);
        }
    }

    @Test
    void test_pathLenConstraint_exceeded_rejected() throws Exception {
        // 1. Issue an unauthorized subordinate CA (violates CA basic constraints / pathLen)
        TestCertificateAuthority unauthorizedSubCa = rootCA.issueUnauthorizedSubCA("CN=Cascara Sub CA 1");

        // 2. Issue a signing identity from the unauthorized CA
        TestSigningIdentity identity = unauthorizedSubCa.issueIdentity("unauthorized-signer");

        // 3. Sign SyntheticJar with this identity
        SyntheticJar jar = SyntheticJar.forUseIn(CertPathValidationTests.class)
                .setPath(getTempPath().resolve("pathlen-violation.jar"))
                .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
                .addDependencies("cascara-common", "integration-test-fixtures")
                .sign(s -> s.withIdentity(identity))
                .packageJar();

        TrustStore trustStore = new TrustStore(Set.of(rootCA.certificate()));
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(trustStore)
                .build();

        try (JarFile jarFile = JarFile.open(jar.getPath())) {
            SignatureStatus status = policy.evaluate(jarFile);

            // Subordinate CA constraint violation must fail trust evaluation
            assertEquals(SignatureStatus.UNTRUSTED_SIGNER, status);
        }
    }


    @Test
    void test_expiredIntermediateCA_rejected() throws Exception {
        // 1. Issue an expired signing identity directly from the authority
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-signer");

        // 2. Sign SyntheticJar with the expired identity
        SyntheticJar jar = SyntheticJar.forUseIn(CertPathValidationTests.class)
                .setPath(getTempPath().resolve("expired-intermediate.jar"))
                .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
                .addDependencies("cascara-common", "integration-test-fixtures")
                .sign(s -> s.withIdentity(expiredIdentity))
                .packageJar();

        TrustStore trustStore = new TrustStore(Set.of(rootCA.certificate()));
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(trustStore)
                .build();

        try (JarFile jarFile = JarFile.open(jar.getPath())) {
            SignatureStatus status = policy.evaluate(jarFile);

            assertEquals(SignatureStatus.CERTIFICATE_EXPIRED, status);
        }
    }
}