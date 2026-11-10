package integration.test.spl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.security.SignatureException;
import io.github.qishr.cascara.common.security.SignatureStatus;
import io.github.qishr.cascara.common.security.SplSignaturePolicy;
import io.github.qishr.cascara.common.security.TrustStore;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceProvider;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.security.TestCertificateAuthority.TestSigningIdentity;

@Isolated
public class SplClassRegistrationSecurityTests extends SplTestBase {

    public static class UnsignedFixtureService implements ServiceProvider {

    }

    @BeforeEach
    protected void setUp() throws IOException {
        super.setUp();
        System.setProperty(SPL.CASCARA_SPL_VERIFY, "true");
    }

    @AfterEach
    protected void tearDown() throws IOException {
        SPL.setSignaturePolicy(SplSignaturePolicy.builder().build());
        System.setProperty(SPL.CASCARA_SPL_VERIFY, "false");
        super.tearDown();
    }

    // @AfterAll
    // protected static void finish() {
    //     System.setProperty(SPL.CASCARA_SPL_VERIFY, "false");
    // }

    @Test
    void test_registerClass_unsignedClass_rejectedWhenRequireSigned() {
        // Build policy with requireSignedClasses = true
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSignedClasses(true)
                .build();

        SPL.setSignaturePolicy(policy);
        SPL layer = spl.create("testLayer");

        // Attempting to register an un-signed class (from current test classpath directory)
        SignatureException ex = assertThrows(SignatureException.class, () -> {
            layer.registerClass(UnsignedFixtureService.class);
        });

        assertEquals(SignatureStatus.NOT_SIGNED, ex.getSignatureStatus());
    }

    @Test
    void test_registerClass_signedClass_acceptedWhenTrusted() throws Exception {
        TestSigningIdentity identity = rootCA.issueIdentity("test-signer");

        // 1. Create and sign a synthetic JAR containing a provider class
        SyntheticJar signedJar = SyntheticJar.forUseIn(SplClassRegistrationSecurityTests.class)
                .setPath(getTempDir().resolve("signed-provider.jar"))
                .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
                .addDependencies("cascara-common", "integration-test-fixtures")
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

    @Test
    void test_registerClass_lenientPolicy_allowsUnsignedClass() {
        // Default policy: requireSignedClasses = false
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSignedClasses(false)
                .build();

        SPL layer = SPL.getRoot().create("testLayer");
        SPL.setSignaturePolicy(policy);

        assertDoesNotThrow(() -> layer.registerClass(UnsignedFixtureService.class));
    }

    @Test
    void test_registerClass_proxyOrDynamicClass_rejectedWhenRequireSigned() {
        SplSignaturePolicy policy = SplSignaturePolicy.builder()
                .requireSignedClasses(true)
                .build();

        SPL.setSignaturePolicy(policy);
        SPL layer = spl.create("testLayer");

        // Generate a dynamic proxy class with no CodeSource location
        Class<?> proxyClass = Proxy.getProxyClass(
                SplClassRegistrationSecurityTests.class.getClassLoader(),
                ServiceProvider.class
        );

        SignatureException ex = assertThrows(SignatureException.class, () -> {
            layer.registerClass(proxyClass);
        });

        assertEquals(SignatureStatus.NOT_SIGNED, ex.getSignatureStatus());
    }

    @Test
    void test_registerClass_dynamicClassWithoutSignedJar_rejectedWhenRequireSigned() {
    SplSignaturePolicy policy = SplSignaturePolicy.builder()
            .requireSignedClasses(true)
            .build();

    SPL.setSignaturePolicy(policy);
    SPL layer = spl.create("testLayer");

    // Create a dynamic proxy instance with a working handler so SPL's property checks pass
    ServiceProvider proxyInstance = (ServiceProvider) Proxy.newProxyInstance(
            SplClassRegistrationSecurityTests.class.getClassLoader(),
            new Class<?>[]{ ServiceProvider.class },
            (proxy, method, args) -> {
                if ("getServiceProperties".equals(method.getName())) {
                    return Map.of(); // Return empty properties map cleanly
                }
                return null;
            }
    );

    // Register the class of the dynamic proxy
    Class<?> dynamicClass = proxyInstance.getClass();

    SignatureException ex = assertThrows(SignatureException.class, () -> {
        layer.registerClass(dynamicClass);
    });

    assertEquals(SignatureStatus.NOT_SIGNED, ex.getSignatureStatus());
}
}