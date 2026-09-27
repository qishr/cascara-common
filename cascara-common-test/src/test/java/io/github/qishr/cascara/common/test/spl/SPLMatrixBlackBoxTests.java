package io.github.qishr.cascara.common.test.spl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class SPLMatrixBlackBoxTests {

    private static final String JAVA_BIN = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    private static String mainClassesDir;
    private static String testResourcesDir;

    @BeforeAll
    static void setupPaths() {
        mainClassesDir = Path.of("build/classes/java/main").toAbsolutePath().toString();
        testResourcesDir = Path.of("build/resources/test").toAbsolutePath().toString();
    }

    @Test
    @DisplayName("Profile 1: Pure Modular Execution (--module-path)")
    public void testProfile1_PureModular() throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(JAVA_BIN);
        cmd.add("--module-path");
        cmd.add(getModulePath());
        cmd.add("--add-modules");
        cmd.add("ALL-MODULE-PATH");
        cmd.add("-m");
        cmd.add("cascara.common.test/io.github.qishr.cascara.common.test.spl.SPLTestMain");

        ProcessResult result = runJvm(cmd);
        assertEquals(0, result.exitCode, "Process failed:\n" + result.output);
        assertDiscovered(result.output, "io.github.qishr.cascara.common.test.spl.TestServiceProvider");
    }

    @Test
    @DisplayName("Profile 2: Modular + IDE Patching (--patch-module with synthetic JDT paths)")
    public void testProfile2_ModularWithJdtPatchModule() throws Exception {
        String syntheticJdtBin = "/cascara-common/bin";
        String patchArg = "cascara.common=" + testResourcesDir + ":" + mainClassesDir + ":" + syntheticJdtBin;

        List<String> cmd = new ArrayList<>();
        cmd.add(JAVA_BIN);
        cmd.add("--patch-module");
        cmd.add(patchArg);
        cmd.add("--module-path");
        cmd.add(getModulePath());
        cmd.add("--add-modules");
        cmd.add("ALL-MODULE-PATH");
        cmd.add("-m");
        cmd.add("cascara.common.test/io.github.qishr.cascara.common.test.spl.SPLTestMain");

        ProcessResult result = runJvm(cmd);
        assertEquals(0, result.exitCode, "Process failed:\n" + result.output);
        assertDiscovered(result.output, "io.github.qishr.cascara.common.test.spl.TestServiceProvider");
    }

    @Test
    @DisplayName("Profile 3: Pure Classpath Execution (-cp / Unnamed Module)")
    public void testProfile3_PureClasspath() throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(JAVA_BIN);
        cmd.add("-cp");
        cmd.add(getClasspath());
        cmd.add("io.github.qishr.cascara.common.test.spl.SPLTestMain");

        ProcessResult result = runJvm(cmd);
        assertEquals(0, result.exitCode, "Process failed:\n" + result.output);
        assertDiscovered(result.output, "io.github.qishr.cascara.common.test.spl.TestServiceProvider");
    }

    @Test
    @DisplayName("Profile 4: Hybrid Classpath + Modulepath Execution")
    public void testProfile4_HybridClasspathModulepath() throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(JAVA_BIN);
        cmd.add("-cp");
        cmd.add(getClasspath());
        cmd.add("--module-path");
        cmd.add(getModulePath());
        cmd.add("--add-modules");
        cmd.add("ALL-MODULE-PATH");
        cmd.add("io.github.qishr.cascara.common.test.spl.SPLTestMain");

        ProcessResult result = runJvm(cmd);
        assertEquals(0, result.exitCode, "Process failed:\n" + result.output);
        assertDiscovered(result.output, "io.github.qishr.cascara.common.test.spl.TestServiceProvider");
    }

    // --- Helpers ---

    private String getModulePath() {
        return System.getProperty("jdk.module.path", "build/modulepath:" + mainClassesDir);
    }

    // private String getClasspath() {
    //     return System.getProperty("java.class.path");
    // }

    private String getClasspath() {
        String baseCp = System.getProperty("java.class.path");

        String testClasses = Path.of("build/classes/java/test").toAbsolutePath().toString();
        String mainClasses = Path.of("build/classes/java/main").toAbsolutePath().toString();

        return baseCp + File.pathSeparator + testClasses + File.pathSeparator + mainClasses;
    }

    private ProcessResult runJvm(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.lines().collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();
            return new ProcessResult(exitCode, output);
        }
    }

    private void assertDiscovered(String output, String fqcn) {
        assertTrue(output.contains("REGISTERED: " + fqcn),
                "Expected provider " + fqcn + " was not discovered.\nFull output:\n" + output);
    }

    private record ProcessResult(int exitCode, String output) {}
}