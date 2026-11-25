package integration.test.spl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import integration.test.exec.ExecTestBase;
import integration.test.jar.JarSignatureInvalidTests;
import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.security.policy.SplSignaturePolicy;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public class SplPolicyTests extends ExecTestBase {
    @Test
    @Tag("requires-jpms")
    void test_accept_splWithoutPolicy_expiredCert() throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");

        // 2. Package and sign JAR
        Path jarPath = Cascara.get().getModulePath().resolve("provider-a.jar");
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies(getDependencies())
            .sign(s -> s.withIdentity(expiredIdentity))
            .packageJar(jarPath);

        // 3. Configure policy requiring valid signatures trusted by rootCA
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSigned(true)
                .setTrustStore(new TrustStore(Set.of(rootCA.certificate())))
                .build();

        System.setProperty(SP_CASCARA_SPL_VERIFY, "false");
        SPL.setSignaturePolicy(policy);
        SPL layer = spl.create("testLayer");
        try {
            assertDoesNotThrow(() -> layer.registerJar(jarPath));
        } finally {
            spl.remove("testLayer");
        }
    }
}
