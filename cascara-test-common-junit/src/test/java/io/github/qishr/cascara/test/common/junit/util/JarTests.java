package io.github.qishr.cascara.test.common.junit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;

public class JarTests extends MetaTestBase {
    @Test
    void testOpenSyntheticJar() throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(getClass())
            .setPath(getTempPath().resolve("jar-tests.jar"))
            .addSources("code/ModuleInfoA.txt")
            .addSources("code/ProviderA.txt")
            .packageJar();

        try (JarFile jar = JarFile.open(testJar.getPath())) {
            assertEquals("provider.a", jar.getModuleName());
            Set<String> classNames = jar.getClassNames();
            assertTrue(classNames.contains("com.example.providera.ProviderA"));
        }
    }
}
