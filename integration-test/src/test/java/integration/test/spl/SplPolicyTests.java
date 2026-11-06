package integration.test.spl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import integration.test.jar.JarSignatureInvalidTests;
import io.github.qishr.cascara.common.security.SignatureException;
import io.github.qishr.cascara.common.security.SignatureStatus;
import io.github.qishr.cascara.common.security.SplSignaturePolicy;
import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public class SplPolicyTests extends SplTestBase {
    @Test
    void test_reject_splWithPolicy_expiredCert(@TempDir Path tempDir) throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");

        // 2. Package and sign JAR
        Path jarPath = Cascara.instance().getModulePath().resolve("provider-a.jar");
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign(s -> s.withIdentity(expiredIdentity))
            .packageJar(jarPath);

        // 3. Configure policy requiring valid signatures trusted by rootCA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .trustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        System.setProperty(SPL.CASCARA_SPL_VERIFY, "true");
        SPL.setSignaturePolicy(policy);
        SPL layer = spl.create("testLayer");
        try {
            ServiceException e = assertThrows(ServiceException.class, () -> layer.registerJar(jarPath));
            assertInstanceOf(SignatureException.class, e.getCause());
            SignatureException se = (SignatureException) e.getCause();
            assertEquals(SignatureStatus.CERTIFICATE_EXPIRED, se.getSignatureStatus());
        } finally {
            spl.remove("testLayer");
        }
    }

    @Test
    void test_accept_splWithoutPolicy_expiredCert(@TempDir Path tempDir) throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");

        // 2. Package and sign JAR
        Path jarPath = Cascara.instance().getModulePath().resolve("provider-a.jar");
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign(s -> s.withIdentity(expiredIdentity))
            .packageJar(jarPath);

        // 3. Configure policy requiring valid signatures trusted by rootCA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .trustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        System.setProperty(SPL.CASCARA_SPL_VERIFY, "false");
        SPL.setSignaturePolicy(policy);
        SPL layer = spl.create("testLayer");
        try {
            assertDoesNotThrow(() -> layer.registerJar(jarPath));
        } finally {
            spl.remove("testLayer");
        }
    }
}
