package integration.test.spl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.UnsignedProviderFixture;
import integration.test.fixtures.task.SplRegisterClassTestTask;
import integration.test.fixtures.task.SplRegisterDynamicClassTestTask;
import integration.test.fixtures.task.SplRegisterProxyClassTestTask;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.exec.IsolatedExecutor;
import io.github.qishr.cascara.common.exec.JvmConfig;
import io.github.qishr.cascara.common.security.cert.TrustStore;
import io.github.qishr.cascara.common.security.policy.SignatureException;
import io.github.qishr.cascara.common.security.policy.SignatureStatus;
import io.github.qishr.cascara.common.security.policy.SplSignaturePolicy;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;
import io.github.qishr.cascara.test.common.util.TestCascara;

@Isolated
public class SplClassRegistrationSecurityTests extends ExecTestBase {

    @BeforeEach
    protected void setUp() {
        super.setUp();

        // We want this process's GlobalReporter to receive diagnostics, but not display them.
        GlobalReporter.get().setSystemOutputEnabled(false);

        System.setProperty(SP_CASCARA_SPL_VERIFY, "true");
    }

    @AfterEach
    protected void tearDown() throws IOException {
        SPL.setSignaturePolicy(SplSignaturePolicy.builder().build());
        System.setProperty(SP_CASCARA_SPL_VERIFY, "false");
        super.tearDown();
    }

    @Test
    void test_registerClass_unsignedClass_rejectedWhenRequireSigned() throws Throwable {
        try (TestCascara cascara = new TestCascara()) {
            IsolatedExecutor exec = IsolatedExecutor.forTask(SplRegisterClassTestTask.class, Boolean.class)
                .setDiagnosticForwarding(true)
                .setJvmConfig(
                    new JvmConfig()
                        .setModuleName(SplRegisterClassTestTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        .setSystemProperty("cascara.report.level." + SplRegisterClassTestTask.class.getName(), "debug")
                        .setSystemProperty("cascara.report.level." + SplSignaturePolicy.class.getName(), "debug")
                        .setSystemProperty(SP_CASCARA_SPL_VERIFY, true)
                        .setSystemProperty(SP_CASCARA_SPL_POLICY_REQUIRE_SIGNED_CLASSES, true)
                        .setTimeout(Duration.ofMinutes(2))
                );


            // exec.run(UnsignedProviderFixture.class.getName());

            // TODO: Is this supposed to be signature exception or service exception?

            SignatureException ex = assertThrows(SignatureException.class, () -> {
                exec.run(UnsignedProviderFixture.class.getName());
            });

            debug(exec.getResponse());

            assertEquals(SignatureStatus.NOT_SIGNED, ex.getSignatureStatus());
        }
    }


    @Test
    void test_registerClass_signedClass_acceptedWhenTrusted() throws Exception {
        try (TestCascara cascara = new TestCascara()) {
            TestSigningIdentity identity = rootCA.issueIdentity("test-signer");

            // 1. Create and sign a synthetic JAR containing a provider class
            SyntheticJar signedJar = SyntheticJar.forUseIn(SplClassRegistrationSecurityTests.class)
                    .setPath(getTempPath().resolve("signed-provider.jar"))
                    .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
                    .addDependencies(getDependencies())
                    // .addDependencies("cascara-common", "integration-test-fixtures")
                    .sign(s -> s.withIdentity(identity))
                    .packageJar();

            // 2. Load class from the signed JAR via a URLClassLoader
            URLClassLoader loader = new URLClassLoader(new URL[]{ signedJar.getPath().toUri().toURL() });
            Class<?> providerClass = Class.forName("com.example.providera.ProviderA", true, loader);

            // Pass a Set of X509Certificates directly
            TrustStore trustStore = new TrustStore(Set.of(identity.certificate(), rootCA.certificate()));

            // 3. Configure policy trusting the test CA
            SplSignaturePolicy policy = SplSignaturePolicy.builder()
                    .requireSignedClasses(true)
                    .setTrustStore(trustStore)
                    .build();

            SPL.setSignaturePolicy(policy);
            SPL layer = spl.create("testLayer");

            // Should succeed without exception
            assertDoesNotThrow(() -> layer.registerClass(providerClass));
        }
    }

    @Test
    void test_registerClass_lenientPolicy_allowsUnsignedClass() {
        // Default policy: requireSignedClasses = false
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSignedClasses(false)
                .build();

        SPL layer = SPL.getRoot().create("testLayer");
        SPL.setSignaturePolicy(policy);

        assertDoesNotThrow(() -> layer.registerClass(UnsignedProviderFixture.class));
    }

    @Test
    void test_registerClass_proxyOrDynamicClass_rejectedWhenRequireSigned() throws Throwable {
        try (TestCascara cascara = new TestCascara()) {
            IsolatedExecutor exec = IsolatedExecutor.forTask(SplRegisterProxyClassTestTask.class, Boolean.class)
                .setDiagnosticForwarding(true)
                .setJvmConfig(
                    new JvmConfig()
                        .setModuleName(SplRegisterClassTestTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        .setSystemProperty("cascara.report.level." + SplRegisterClassTestTask.class.getName(), "debug")
                        .setSystemProperty("cascara.report.level." + SplSignaturePolicy.class.getName(), "debug")
                        .setSystemProperty(SP_CASCARA_SPL_VERIFY, true)
                        .setSystemProperty(SP_CASCARA_SPL_POLICY_REQUIRE_SIGNED_CLASSES, true)
                        .setTimeout(Duration.ofMinutes(2))
                );

            SignatureException ex = assertThrows(SignatureException.class, () -> {
                exec.run(UnsignedProviderFixture.class.getName());
            });

            debug(exec.getResponse());

            assertEquals(SignatureStatus.NOT_SIGNED, ex.getSignatureStatus());
        }
    }

    // ****************************************
    // TODO: This needs to use IsolatedExecutor
    // ****************************************
    // @Test
    // void test_registerClass_dynamicClassWithoutSignedJar_rejectedWhenRequireSigned() {
    //     SplSignaturePolicy policy = SplSignaturePolicy.builder()
    //             .requireSignedClasses(true)
    //             .build();

    //     SPL.setSignaturePolicy(policy);
    //     SPL layer = spl.create("testLayer");

    //     // Create a dynamic proxy instance with a working handler so SPL's property checks pass
    //     ServiceProvider proxyInstance = (ServiceProvider) Proxy.newProxyInstance(
    //             SplClassRegistrationSecurityTests.class.getClassLoader(),
    //             new Class<?>[]{ ServiceProvider.class },
    //             (proxy, method, args) -> {
    //                 if ("getServiceProperties".equals(method.getName())) {
    //                     return Map.of(); // Return empty properties map cleanly
    //                 }
    //                 return null;
    //             }
    //     );

    //     // Register the class of the dynamic proxy
    //     Class<?> dynamicClass = proxyInstance.getClass();

    //     SignatureException ex = assertThrows(SignatureException.class, () -> {
    //         layer.registerClass(dynamicClass);
    //     });

    //     assertEquals(SignatureStatus.NOT_SIGNED, ex.getSignatureStatus());
    // }

    @Test
    void test_registerClass_dynamicClassWithoutSignedJar_rejectedWhenRequireSigned() throws Throwable {
        try (TestCascara cascara = new TestCascara()) {
            IsolatedExecutor exec = IsolatedExecutor.forTask(SplRegisterDynamicClassTestTask.class, Boolean.class)
                .setDiagnosticForwarding(true)
                .setJvmConfig(
                    new JvmConfig()
                        .setModuleName(SplRegisterClassTestTask.class.getModule().getName())
                        .setModulePath(getJvmModulePath())
                        .setEnv(EV_CASCARA_HOME, cascara.getHomePath())
                        .setSystemProperty("cascara.report.level." + SplRegisterClassTestTask.class.getName(), "debug")
                        .setSystemProperty("cascara.report.level." + SplSignaturePolicy.class.getName(), "debug")
                        .setSystemProperty(SP_CASCARA_SPL_VERIFY, true)
                        .setSystemProperty(SP_CASCARA_SPL_POLICY_REQUIRE_SIGNED_CLASSES, true)
                        .setTimeout(Duration.ofMinutes(2))
                );

            SignatureException ex = assertThrows(SignatureException.class, () -> {
                exec.run(UnsignedProviderFixture.class.getName());
            });

            debug(exec.getResponse());

            assertEquals(SignatureStatus.NOT_SIGNED, ex.getSignatureStatus());
        }
    }
}