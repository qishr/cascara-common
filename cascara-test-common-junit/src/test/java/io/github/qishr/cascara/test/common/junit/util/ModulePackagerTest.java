package io.github.qishr.cascara.test.common.junit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;

public class ModulePackagerTest extends MetaTestBase {
    @Test
    void testModulePackager() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(ModulePackagerTest.class)
            .setPath(getTempDir().resolve("provider.jar"))
            // .setMinCascaraVersion("0.14.0")
            .addSources(sourceCode())
            // .addSources("/java/Main.txt", "/java/module-info.txt")
            .addDependencies("cascara-common", "cascara-test-common-junit")
            .packageJar();

        // Check the JAR can be opened
        try (JarFile jar = JarFile.open(testJar.getPath())) {
            // assertNotNull(jar, "Failed to create valid JAR file");
            assertEquals("provider.a", jar.getModuleName());
        } finally {
            Files.delete(testJar.getPath());
        }
    }
}
