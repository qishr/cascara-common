package integration.test.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Set;

import org.junit.jupiter.api.Test;

import integration.test.spl.SplTestBase;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;

public class JarFileTests extends SplTestBase {
    @Test
    void testOpenSyntheticJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(getClass())
            .setPath(createTempFile())
            .addSources("code/ModuleInfoA.txt", "code/ProviderA.txt")
            .addDependencies("cascara-common", "integration-test-fixtures")
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            assertEquals("provider.a", jar.getModuleName());
            Set<String> classNames = jar.getClassNames();
            assertTrue(classNames.contains("com.example.providera.ProviderA"));
        }
    }
}
