package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.qishr.cascara.common.semver.SemVer;

public class EnvironmentAwareTests {
    static boolean isModularEnvironment() {
        return EnvironmentAwareTests.class.getModule().isNamed();
    }

    @Test
    void test_cascaraVersionAllEnvironments() throws Exception {
        SemVer cascaraVersion = Cascara.get().getVersion();
        assertNotNull(cascaraVersion);
        assertNotEquals("0.0.0", cascaraVersion.toString());
    }

    // TODO: Uncomment these and make them use synthetic / virtual / test JARs

    /// This test runs in these environments:
    /// - JAR file with JPMS
    @Test
    @Tag("requires-jar")
    @EnabledIf("isModularEnvironment")
    void test_cascaraVersionWithModularJar() throws Exception {
        SemVer cascaraVersion = Cascara.get().getVersion();
        assertNotNull(cascaraVersion);
        assertNotEquals("0.0.0", cascaraVersion.toString());
    }

    /// This test runs in these environments:
    /// - JAR file with JPMS
    /// - JAR file without JPMS
    @Test
    @Tag("requires-jar")
    void test_cascaraVersionWithJar() throws Exception {
        SemVer cascaraVersion = Cascara.get().getVersion();
        assertNotNull(cascaraVersion);
        assertNotEquals("0.0.0", cascaraVersion.toString());
    }

    /// This test runs in these environments:
    /// - No JAR file, with JPMS
    /// - No JAR file, without JPMS
    @Test
    @Tag("no-jar")
    void test_cascaraVersionWithoutJar() throws Exception {
        SemVer cascaraVersion = Cascara.get().getVersion();
        assertNotNull(cascaraVersion);
        assertNotEquals("0.0.0", cascaraVersion.toString());
    }

    @Test
    @Tag("no-jar")
    void test_commonVersionWithoutJar() throws Exception {
        SemVer jarCommonVersion = Cascara.get().getCommonVersion();
        assertNotNull(jarCommonVersion);
        assertNotEquals("0.0.0", jarCommonVersion.toString());
    }

    @Test
    @Tag("requires-jar")
    void test_commonVersionWithJar() throws Exception {
        SemVer jarCommonVersion = Cascara.get().getCommonVersion();
        assertNotNull(jarCommonVersion);
        assertNotEquals("0.0.0", jarCommonVersion.toString());
    }

    @Test
    @Tag("requires-jar")
    @EnabledIf("isModularEnvironment")
    void test_commonVersionWithModularJar() throws Exception {
        SemVer jarCommonVersion = Cascara.get().getCommonVersion();
        assertNotNull(jarCommonVersion);
        assertNotEquals("0.0.0", jarCommonVersion.toString());
    }
}
