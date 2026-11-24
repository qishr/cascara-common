package integration.test.util;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.util.Cascara;

public class EnvironmentAwareTests {
    static boolean isModularEnvironment() {
        return EnvironmentAwareTests.class.getModule().isNamed();
    }

    /// This test runs in these environments:
    /// - JAR file with JPMS
    @Test
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
    void test_cascaraVersionWithJar() throws Exception {
        SemVer cascaraVersion = Cascara.get().getVersion();
        assertNotNull(cascaraVersion);
        assertNotEquals("0.0.0", cascaraVersion.toString());
    }

    @Test
    void test_commonVersionWithJar() throws Exception {
        SemVer jarCommonVersion = Cascara.get().getCommonVersion();
        assertNotNull(jarCommonVersion);
        assertNotEquals("0.0.0", jarCommonVersion.toString());
    }

    @Test
    @EnabledIf("isModularEnvironment")
    void test_commonVersionWithModularJar() throws Exception {
        SemVer jarCommonVersion = Cascara.get().getCommonVersion();
        assertNotNull(jarCommonVersion);
        assertNotEquals("0.0.0", jarCommonVersion.toString());
    }
}
