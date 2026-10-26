package integration.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.TestService;
import integration.test.spl.SplTestBase;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.security.JarManifestVerifier;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.trackable.tracker.ArrayChangeTracker;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.junit.util.SyntheticJar;
import io.github.qishr.cascara.test.common.junit.util.TestModulePackager;

public class JarSigningTests extends SplTestBase {
    @Test
    void testSignSyntheticJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(JarSigningTests.class)
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        String layerName = "signingTestLayer";
        SPL layer = spl.create(layerName);

        layer.registerJar(testJar.getPath());

        // Verify the provider is registered
        List<ServiceMetadata> providers = spl.findAllProviders(TestService.class);
        assertEquals(1, providers.size());
        ServiceMetadata providerMetadata = providers.getFirst();
        assertEquals("ProviderA", providerMetadata.getProperties().getString("providerName"));

        // Remove the layer
        spl.remove(layerName);
    }

    @Test
    void test_loadingAndUnloading() throws IOException {
        String layerName = "signingTestLayer";
        SPL layer = spl.create(layerName);

        reporter.info(GenericMessage.INFO, "Test building JAR");

        // This path is in the VFS...
        Path providerJar = Cascara.getModulePath().resolve("provider.jar");

        TestModulePackager.packageModuleJar(
            providerJar,
            "0.14.0",
            sourceCodeA(),
            List.of("cascara-common", "integration-test-fixtures"),
            "integration-test"
        );

        // TODO: Sign it


        reporter.info(GenericMessage.INFO, "Test registering JAR");
        layer.registerJar(providerJar);

        // Verify the provider is registered
        List<ServiceMetadata> providers = spl.findAllProviders(TestService.class);
        assertEquals(1, providers.size());
        ServiceMetadata providerMetadata = providers.getFirst();
        assertEquals("ProviderA", providerMetadata.getProperties().getString("providerName"));

        // Remove the layer
        spl.remove(layerName);
    }
}
