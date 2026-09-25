package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.qishr.cascara.common.semver.SemVer;

public class EnvironmentAwareTests {
    static boolean isModularEnvironment() {
        return EnvironmentAwareTests.class.getModule().isNamed();
    }

    /// This test runs in these environments:
    /// - JAR file with JPMS
    @Test
    @Tag("requires-jar")
    @EnabledIf("isModularEnvironment")
    void test_cascaraVersionWithModularJar() throws Exception {
        SemVer jarCascaraVersion;

        Path jarPath = GradleArtifactResolver.resolveJarPath("cascara-common");
        try (JarFile jarFile = JarFile.open(jarPath)) {
            jarCascaraVersion = new SemVer(jarFile.getManifest().get("Cascara-Version"));
        }
        assertNotNull(jarCascaraVersion);
        assertNotEquals("0.0.0", jarCascaraVersion.toString());

        SemVer cascaraVersion = Cascara.getVersion();
        assertEquals(jarCascaraVersion, cascaraVersion);
    }

    /// This test runs in these environments:
    /// - JAR file with JPMS
    /// - JAR file without JPMS
    @Test
    @Tag("requires-jar")
    void test_cascaraVersionWithJar() throws Exception {
        SemVer jarCascaraVersion;

        Path jarPath = GradleArtifactResolver.resolveJarPath("cascara-common");
        try (JarFile jarFile = JarFile.open(jarPath)) {
            jarCascaraVersion = new SemVer(jarFile.getManifest().get("Cascara-Version"));
        }
        assertNotNull(jarCascaraVersion);
        assertNotEquals("0.0.0", jarCascaraVersion.toString());

        SemVer cascaraVersion = Cascara.getVersion();
        assertEquals(jarCascaraVersion, cascaraVersion);
    }

    @Test
    @Tag("requires-jar")
    void test_commonVersionWithJar() throws Exception {
        SemVer jarCommonVersion;
        Path jarPath = GradleArtifactResolver.resolveJarPath("cascara-common");
        try (JarFile jarFile = JarFile.open(jarPath)) {
            jarCommonVersion = new SemVer(jarFile.getManifest().get("Cascara-Version"));
        }
        assertNotNull(jarCommonVersion);
        assertNotEquals("0.0.0", jarCommonVersion.toString());
    }

    /// This test runs in these environments:
    /// - No JAR file, with JPMS
    /// - No JAR file, without JPMS
    @Test
    @Tag("no-jar")
    void test_cascaraVersionWithoutJar() throws Exception {
        // SemVer cascaraVersion = Cascara.getVersion();
        // assertEquals("0.0.0", cascaraVersion.toString());
        // // assertEquals("0.11.0", cascaraVersion.toString());

        SemVer jarCascaraVersion;

        Path jarPath = GradleArtifactResolver.resolveJarPath("cascara-common");
        try (JarFile jarFile = JarFile.open(jarPath)) {
            jarCascaraVersion = new SemVer(jarFile.getManifest().get("Cascara-Version"));
        }
        assertNotNull(jarCascaraVersion);
        assertNotEquals("0.0.0", jarCascaraVersion.toString());

        SemVer cascaraVersion = Cascara.getVersion();
        assertEquals(jarCascaraVersion, cascaraVersion);
    }
}
