package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import integration.test.spl.SplTestBase;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JarFile.JarEntry;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;

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

}
