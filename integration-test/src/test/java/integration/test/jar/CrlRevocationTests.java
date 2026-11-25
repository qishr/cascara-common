package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.X509CRL;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.security.policy.SplSignaturePolicy;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.junit.base.TestBase;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

class CrlRevocationTests extends TestBase {

    private static TestCertificateAuthority ca;
    private static TestSigningIdentity activeSigner;
    private static TestSigningIdentity revokedSigner;
    private static X509CRL crl;

    // TODO: Make this extend JarTestBase and use it's rootCA

    @BeforeAll
    static void init() throws Exception {
        ca = TestCertificateAuthority.create("Revocation CA");
        activeSigner = ca.issueIdentity("active-module");
        revokedSigner = ca.issueIdentity("revoked-module");

        // Generate a CRL revoking revokedSigner's serial number
        crl = ca.generateCrl(Set.of(revokedSigner.certificate().getSerialNumber()));
    }

    @Test
    void test_activeCert_notRevokedInCrl() {
        assertFalse(crl.isRevoked(activeSigner.certificate()));
    }

    @Test
    void test_revokedCert_detectedInCrl() {
        assertTrue(crl.isRevoked(revokedSigner.certificate()));
    }

    @Test
    void test_policyEvaluation_failsForRevokedModuleJar() throws Exception {
        // Sign JAR with revoked identity
        SyntheticJar jar = SyntheticJar.forUseIn(CrlRevocationTests.class)
            .setPath(getTempPath().resolve("revoked.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
            // .signWith(revokedSigner)
            .sign(s -> s.withIdentity(revokedSigner))
            .packageJar();

        SplSignaturePolicy policy = SplSignaturePolicy.builder()
            .requireSigned(true)
            .setTrustStore(new TrustStore(Set.of(ca.certificate())))
            .addCrl(crl) // or enable CRL checking via PKIXParameters
            .build();

        try (JarFile jarFile = JarFile.open(jar.getPath())) {
            SignatureStatus status = policy.evaluate(jarFile);
            assertEquals(SignatureStatus.CERTIFICATE_REVOKED, status);
        }
    }
}