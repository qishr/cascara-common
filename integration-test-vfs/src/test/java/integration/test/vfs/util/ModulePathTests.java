package integration.test.vfs.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.qishr.cascara.common.util.ModulePath;
import io.github.qishr.cascara.test.common.jar.SyntheticJar;
import io.github.qishr.cascara.test.common.junit.base.VfsTestBase;

public class ModulePathTests extends VfsTestBase {

    private static boolean isJpmsEnvironment() {
        String moduleName = ModulePathTests.class.getModule().getName();
        return moduleName != null && !moduleName.isEmpty();
    }

    public static abstract class TestBase {
        public String x;
    }

    public static class TestImpl extends TestBase {
        public String y;
    }

    @Test
    void test_findsTestClass() {
        if (isJpmsEnvironment()) {
            ModulePath mp = new ModulePath();
            assertTrue(mp.containsClass(TestBase.class.getName()));
        } else {
            // REPORTER.debug("Skipping testModulePath in non-JPMS environment");
        }
    }

    @Test
    void test_doesNotFindExcludedClass() {
        if (isJpmsEnvironment()) {
            Set<String> excluded = Set.of(
                "org.mockito.", "jdk.", "sun.", "com.sun.", "net.bytebuddy.",
                "worker.org.gradle.", "org.objenesis.", "org.opentest4j.",
                "org.jcp.xml.dsig.internal.", "org.junit",
                "com.apple", "apple.laf", "apple.security",
                "java.awt.", "javax.swing."
            );

            ModulePath mp = new ModulePath(null, null, excluded, null);

            for (String ex : excluded) {
                assertFalse(mp.containsClass(ex), ex + " should have been excluded");
            }
        } else {
            // REPORTER.debug("Skipping testModulePath in non-JPMS environment");
        }
    }

    @Test
    void test_processClassFile_readsModuleInfoAndSetsSiblingModuleName(@TempDir Path classDir) throws Exception {
        SyntheticJar testJar = SyntheticJar.forUseIn(getClass())
            .setPath(getTempPath().resolve("lr-moduleinfo.jar"))
            .addSources("code/sigtest/module-info.txt", "code/sigtest/SigTest.txt")
            .packageJar();

        byte[] moduleInfo = testJar.getClasses().get("module-info.class");
        Path moduleInfoPath = classDir.resolve("module-info.class");
        Files.write(moduleInfoPath, moduleInfo);

        // 2. Instantiate real ModulePath
        ModulePath modulePath = new ModulePath(classDir.toString());

        // 3. Assert public state/behavior
        assertTrue(modulePath.containsModule("provider.a"));
    }
}
