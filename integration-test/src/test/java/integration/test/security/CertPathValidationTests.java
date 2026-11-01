package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.security.cert.CertPathValidationResult;
import io.github.qishr.cascara.common.security.cert.CertPathVerifier;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

class CertPathValidationTests {

    private static TestCertificateAuthority rootCA;
    private static TestCertificateAuthority intermediateCA;
    private static TestCertificateAuthority untrustedCA;

    @BeforeAll
    static void setUp() throws Exception {
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
}