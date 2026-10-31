package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import integration.test.fixtures.TestService;
import integration.test.spl.SplTestBase;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceMetadata;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarFile.JarEntry;
import io.github.qishr.cascara.test.common.junit.util.SyntheticJar;

public class JarSigningTests extends SplTestBase {

    // TODO: Make test common junit a subproject of this

    @Test
    void testSignSyntheticJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(getClass())
            .setPath(getTempDir().resolve("provider.jar"))
            .addSources(sourceCodeA())
            // XXX test with incorrect deps
            .addDependencies("cascara-common", "integration-test-fixtures")
            .sign()
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            byte[] mf = jar.extractFile("META-INF/MANIFEST.MF");
            byte[] sf = jar.extractFile("META-INF/MANIFEST.SF");
            JarManifestVerifier jv = new JarManifestVerifier(mf, sf);
            assertTrue(jv.verifySignatureFileAgainstManifest());
            for (JarEntry jarEntry : jar.getJarEntries()) {
                // Certificate[] certs = jarEntry.getCertificates();
                try (InputStream is = jar.getInputStream(jarEntry)) {
                    assertTrue(jv.verifyEntryContent(jarEntry.getName(), is));
                }
            }
        }
    }

    @Test
    void testLoadSignedJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(getClass())
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
}
