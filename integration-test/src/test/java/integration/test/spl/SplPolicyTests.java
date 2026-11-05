package integration.test.spl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Disabled;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.payload.ReporterTestInput;
import integration.test.fixtures.payload.ReporterTestOutput;
import integration.test.fixtures.task.SplPolicyTestTask;
import integration.test.jar.JarSignatureInvalidTests;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.security.SignatureException;
import io.github.qishr.cascara.common.security.SignatureStatus;
import io.github.qishr.cascara.common.security.SplSignaturePolicy;
import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;

public class SplPolicyTests extends ExecTestBase {

    @Test
    void test_splWithPolicy_rejects_expiredCert(@TempDir Path tempDir) throws Exception {
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

        System.setProperty(SPL.CASCARA_SEC_VERIFY, "true");
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
    void test_splWithoutPolicy_accepts_expiredCert(@TempDir Path tempDir) throws Exception {
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

        System.setProperty(SPL.CASCARA_SEC_VERIFY, "false");
        SPL.setSignaturePolicy(policy);
        SPL layer = spl.create("testLayer");
        try {
            assertDoesNotThrow(() -> layer.registerJar(jarPath));
        } finally {
            spl.remove("testLayer");
        }
    }

    @TempDir
    public Path tempDir;

    @Test
    void test_isolatedWithHostFS_splWithPolicy_rejects_expiredCert(@TempDir Path tempDir) throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");

        // 2. Package and sign JAR


        Path jarPath = tempDir.resolve("provider-a.jar");


        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign(s -> s.withIdentity(expiredIdentity))
            .packageJar(jarPath);

        // try(Stream<Path> jarFiles = Files.walk(Cascara.instance().getModulePath())) {
        //     for (Path jarFile : jarFiles.toList()) {
        //         System.out.println("> " + jarFile);
        //     }
        // }

        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
        IsolatedExecutor exec = IsolatedExecutor.forTask(SplPolicyTestTask.class, ReporterTestOutput.class);

        // Enable forwarding of TRACE reports from isolated JVM
        exec.setDiagnosticForwardingEnabled(true);
        GlobalReporter.forClass(SPL.class).setLevel(Level.DEBUG);
        GlobalReporter.globalInstance().getFormatOptions().setIncluedPid(true);

        // Add the JAR to the module path
        String modulePath = getModulePath() + File.pathSeparator + jarPath;

        exec.setOptions(new JvmOptions()
            .setModuleName(SplPolicyTestTask.class.getModule().getName())
            .setModulePath(modulePath)
            .setSystemProperty(SPL.CASCARA_SEC_VERIFY, "true")
            .setSystemProperty(SPL.CASCARA_SPL_POLICY_CHECK_EXPIRY, "true")
            .setSystemProperty("casc.report.level." + SPL.class.getName(), "DEBUG")
            .setSystemProperty("casc.report.level." + SplPolicyTestTask.class.getName(), "DEBUG")
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED));

        Response<ReporterTestOutput> result = exec.run(input);

        debug(result);

        assertTrue(result.getSystemErr().contains("EXPIRED"));
    }

    // TODO: Check this works with cascara.sec.verify = false

    @Disabled
    @Test
    void test_isolatedWithVFS_splWithPolicy_rejects_expiredCert(@TempDir Path tempDir) throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");

        // 2. Package and sign JAR
        Path jarPath = Cascara.instance().getModulePath().resolve("provider-a.jar");
        SyntheticJar.forUseIn(JarSignatureInvalidTests.class)
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign(s -> s.withIdentity(expiredIdentity))
            .packageJar(jarPath);

        syncVfs();
        try(Stream<Path> jarFiles = Files.walk(Cascara.instance().getModulePath())) {
            for (Path jarFile : jarFiles.toList()) {
                System.out.println("> " + jarFile);
            }
        }

        ReporterTestInput input = new ReporterTestInput("hello-isolated-world");
        IsolatedExecutor exec = IsolatedExecutor.forTask(SplPolicyTestTask.class, ReporterTestOutput.class);

        // Enable forwarding of TRACE reports from isolated JVM
        exec.setDiagnosticForwardingEnabled(true);
        GlobalReporter.forClass(SPL.class).setLevel(Level.TRACE);

        // Add the JAR to the module path
        String modulePath = getModulePath() + File.pathSeparator + jarPath;

        exec.setOptions(new JvmOptions()
            .setModuleName(SplPolicyTestTask.class.getModule().getName())
            .setModulePath(modulePath)
            .setSystemProperty(SPL.CASCARA_SEC_VERIFY, "true")
            .setSystemProperty(SPL.CASCARA_SPL_POLICY_CHECK_EXPIRY, "true")
            .setSystemProperty("casc.report.level." + SPL.class.getName(), "DEBUG")
            .setSystemProperty("casc.report.level." + SplPolicyTestTask.class.getName(), "DEBUG")
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED)
            .setEnv("CASC_HOME", getPhysicalHomePath()));

        Response<ReporterTestOutput> result = exec.run(input);

        debug(result);

        assertTrue(result.getSystemErr().contains("EXPIRED"));
    }

}
