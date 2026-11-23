package integration.test.spl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import integration.test.exec.ExecTestBase;
import integration.test.fixtures.TestService;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;

public class SignedJarLoadTests extends ExecTestBase {
    @Test
    void testLoadSignedJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(getClass())
            .setPath(getTempPath().resolve("provider.jar"))
            .addSources("code/providera/module-info.txt", "code/providera/ProviderA.txt")
            .addDependencies(getDependencies())
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

}
