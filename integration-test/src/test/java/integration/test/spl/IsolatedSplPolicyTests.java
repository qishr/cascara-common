package integration.test.spl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.payload.StringTaskInput;
import integration.test.fixtures.payload.ReporterTestOutput;
import integration.test.fixtures.payload.SplRegisterClassTestOutput;
import integration.test.fixtures.task.SplPolicyTestTask;
import integration.test.fixtures.task.SplRegisterClassTestTask;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.IsolatedExecutor.Response;
import io.github.qishr.cascara.common.exec.ipc.IpcServer;
import io.github.qishr.cascara.common.exec.JvmOptions;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

public class IsolatedSplPolicyTests extends ExecTestBase {

    @TempDir
    public Path tempDir;

    @Test
    void test_reject_expiredCert(@TempDir Path tempDir) throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");

        // 2. Package and sign JAR
        Path jarPath = tempDir.resolve("provider-a.jar");

        SyntheticJar.forUseIn(IsolatedSplPolicyTests.class)
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign(s -> s.withIdentity(expiredIdentity))
            .packageJar(jarPath);

        StringTaskInput input = new StringTaskInput("hello-isolated-world");
        IsolatedExecutor exec = IsolatedExecutor.forTask(SplPolicyTestTask.class, ReporterTestOutput.class);

        // Enable forwarding of DEBUG reports from isolated JVM
        exec.setDiagnosticForwardingEnabled(true);
        GlobalReporter.globalInstance().getFormatOptions().setIncluedPid(true);

        // Add the JAR to the module path
        String modulePath = getModulePath() + File.pathSeparator + jarPath;

        exec.setOptions(new JvmOptions()
            .setModuleName(SplPolicyTestTask.class.getModule().getName())
            .setModulePath(modulePath)
            .setEnv(CASCARA_HOME, getPhysicalHomePath())
            .setSystemProperty(SPL.CASCARA_SPL_BOOT_ERRORS_TO_CONSOLE, "true")
            .setSystemProperty(SPL.CASCARA_SPL_VERIFY, "true")
            .setSystemProperty(SPL.CASCARA_SPL_POLICY_CHECK_EXPIRY, "true")
            .setSystemProperty("casc.report.level." + SPL.class.getName(), "DEBUG")
            .setSystemProperty("casc.report.level." + SplPolicyTestTask.class.getName(), "DEBUG")
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED));

        Response<ReporterTestOutput> result = exec.run(input);

        debug(result);

        assertTrue(result.getSystemErr().contains("EXPIRED"));
    }

    // The above, but with cascara.sec.verify = false
    @Test
    void test_accept_expiredCert(@TempDir Path tempDir) throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");

        // 2. Package and sign JAR
        Path jarPath = tempDir.resolve("provider-a.jar");

        SyntheticJar.forUseIn(IsolatedSplPolicyTests.class)
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign(s -> s.withIdentity(expiredIdentity))
            .packageJar(jarPath);

        StringTaskInput input = new StringTaskInput("hello-isolated-world");
        IsolatedExecutor exec = IsolatedExecutor.forTask(SplPolicyTestTask.class, ReporterTestOutput.class);

        // Enable forwarding of TRACE reports from isolated JVM
        exec.setDiagnosticForwardingEnabled(true);
        GlobalReporter.globalInstance().getFormatOptions().setIncluedPid(true);

        // Add the JAR to the module path
        String modulePath = getModulePath() + File.pathSeparator + jarPath;

        exec.setOptions(new JvmOptions()
            .setModuleName(SplPolicyTestTask.class.getModule().getName())
            .setModulePath(modulePath)
            .setEnv(CASCARA_HOME, getPhysicalHomePath())
            .setSystemProperty(SPL.CASCARA_SPL_VERIFY, "false")
            .setSystemProperty(SPL.CASCARA_SPL_POLICY_CHECK_EXPIRY, "true")
            .setSystemProperty("casc.report.level." + SPL.class.getName(), "DEBUG")
            .setSystemProperty("casc.report.level." + SplPolicyTestTask.class.getName(), "DEBUG")
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED));

        Response<ReporterTestOutput> result = exec.run(input);

        debug(result);

        assertFalse(result.getSystemErr().contains("Verification failed"));
    }

    // TODO: Fix this
    @Disabled
    @Test
    void test_reject_withVFS__expiredCert(@TempDir Path tempDir) throws Exception {
        // 1. Issue an expired identity
        TestSigningIdentity expiredIdentity = rootCA.issueExpiredIdentity("expired-module");

        // 2. Package and sign JAR
        Path jarPath = Cascara.instance().getModulePath().resolve("provider-a.jar");
        SyntheticJar.forUseIn(IsolatedSplPolicyTests.class)
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

        StringTaskInput input = new StringTaskInput("hello-isolated-world");
        IsolatedExecutor exec = IsolatedExecutor.forTask(SplPolicyTestTask.class, ReporterTestOutput.class);

        // Enable forwarding of TRACE reports from isolated JVM
        exec.setDiagnosticForwardingEnabled(true);
        GlobalReporter.globalInstance().getFormatOptions().setIncluedPid(true);

        // Add the JAR to the module path
        String modulePath = getModulePath() + File.pathSeparator + jarPath;

        exec.setOptions(new JvmOptions()
            .setModuleName(SplPolicyTestTask.class.getModule().getName())
            .setModulePath(modulePath)
            .setEnv(CASCARA_HOME, getPhysicalHomePath())
            .setSystemProperty(SPL.CASCARA_SPL_BOOT_ERRORS_TO_CONSOLE, "true")
            .setSystemProperty(SPL.CASCARA_SPL_VERIFY, "true")
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

    /// Unsigned jar on module path loaded by jvm at boot time
    /// will be rejected by SPL, then try to register it via
    /// SPL.registerClass() in SplRegisterClassTestTask. with
    /// CASCARA_SPL_POLICY_REQUIRE_CLASS, that should alsso fail.
    @Test
    void test_reject_registerClass_unsignedJar(@TempDir Path tempDir) throws Exception {
        Path jarPath = tempDir.resolve("provider-a.jar");

        SyntheticJar.forUseIn(IsolatedSplPolicyTests.class)
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .packageJar(jarPath);

        // Add the JAR to the module path
        String modulePath = getModulePath() + File.pathSeparator + jarPath;

        IsolatedExecutor exec = IsolatedExecutor.forTask(SplRegisterClassTestTask.class, SplRegisterClassTestOutput.class);

        // exec.setIpcDebugEnabled(true);
        GlobalReporter.forClass(IpcServer.class).setLevel(Level.DEBUG);

        // Enable forwarding of DEBUG reports from isolated JVM
        exec.setDiagnosticForwardingEnabled(true);
        GlobalReporter.globalInstance().getFormatOptions().setIncluedPid(true);

        exec.setOptions(new JvmOptions()
            .setModuleName(SplRegisterClassTestTask.class.getModule().getName())
            .setModulePath(modulePath)
            .setEnv(CASCARA_HOME, getPhysicalHomePath())
            .setSystemProperty(SPL.CASCARA_SPL_VERIFY, "true")
            .setSystemProperty(SPL.CASCARA_SPL_POLICY_CHECK_EXPIRY, "true")
            .setSystemProperty(SPL.CASCARA_SPL_POLICY_REQUIRE_SIGNED, "true")
            .setSystemProperty(SPL.CASCARA_SPL_POLICY_REQUIRE_SIGNED_CLASS, "true")
            .setSystemProperty("casc.report.level." + SPL.class.getName(), "DEBUG")
            .setSystemProperty("casc.report.level." + SplRegisterClassTestTask.class.getName(), "DEBUG")
            .setTimeout(Duration.ofSeconds(20))
            .setDebug(PROCESS_DEBUG_ENABLED));

        StringTaskInput input = new StringTaskInput("com.example.providera.ProviderA");
        Response<SplRegisterClassTestOutput> result = exec.run(input);

        debug(result);

        assertTrue(result.getSystemErr().contains("NOT_SIGNED"));
    }
}
