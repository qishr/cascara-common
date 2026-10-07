// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

package integration.test.spl;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SPLMatrixBlackBoxTests {

    @Disabled
    @Test
    @DisplayName("Replicate IDE --patch-module scanner leak with external -cp classes")
    void testProfile_IDEPatchModuleWithClasspathLeak() throws Exception {
        String javaBin = ProcessHandle.current().info().command().orElse("java");
        String testClasses = Path.of("build/classes/java/test").toAbsolutePath().toString();
        String mainClasses = Path.of("build/classes/java/main").toAbsolutePath().toString();
        String commonMainClasses = Path.of("../cascara-common/build/classes/java/main").toAbsolutePath().toString();
        String testInterfaces = Path.of("../module-test-interfaces/build/classes/java/main").toAbsolutePath().toString();

        // 1. Dynamically capture ALL runtime dependencies from Gradle/JVM runtime properties
        Set<String> fullRuntimeEntries = resolveFullRuntimePath();

        // 2. Build composite module path combining local build outputs and runtime libraries
        Set<String> modulePathEntries = new LinkedHashSet<>();
        modulePathEntries.add(testClasses);
        modulePathEntries.add(mainClasses);
        modulePathEntries.add(commonMainClasses);
        modulePathEntries.add(testInterfaces);
        modulePathEntries.addAll(fullRuntimeEntries);

        List<String> command = new ArrayList<>();
        command.add(javaBin);

        // --module-path with full runtime set
        command.add("--module-path");
        command.add(String.join(File.pathSeparator, modulePathEntries));

        // --patch-module configuration
        command.add("--patch-module");
        command.add("cascara.common.test=" + testClasses);

        command.add("--patch-module");
        command.add("test.interfaces=" + testInterfaces);

        // Module exports / reads
        command.add("--add-modules=ALL-MODULE-PATH");
        command.add("--add-reads");
        command.add("cascara.common.test=ALL-UNNAMED");

        // Force external dependency JARs onto -cp to simulate the IDE scanner leak scenario
        String cpString = String.join(File.pathSeparator, fullRuntimeEntries);
        command.add("-cp");
        command.add(cpString);

        // Debug output dump
        for (String arg : command) {
            System.out.println("      " + arg);
        }

        // Target main inside named module
        command.add("-m");
        command.add("cascara.common.test/io.github.qishr.cascara.common.test.spl.SPLTestMain");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();

        // boolean scannedForeignClasses = output.toString().contains("scanPackageForProviders: org.junit")
        //         || output.toString().contains("scanPackageForProviders: org.eclipse.jdt");

        // assertTrue(scannedForeignClasses, "Expected scanner to leak into external -cp packages during scanViaClassLoader fallback.\nOutput:\n" + output);
        // assertEquals(0, exitCode, "Process failed with output:\n" + output);


        boolean registeredTestProvider = output.toString().contains("REGISTERED: io.github.qishr.cascara.common.test.spl.TestServiceProvider");

        assertTrue(registeredTestProvider, "Expected TestServiceProvider to be discovered and registered.\nOutput:\n" + output);
        assertEquals(0, exitCode, "Process failed with output:\n" + output);

    }

    private Set<String> resolveFullRuntimePath() {
        Set<String> paths = new LinkedHashSet<>();

        // Check standard class path
        addPathProperty(paths, System.getProperty("java.class.path"));

        // Check JPMS module path set by Gradle test runner
        addPathProperty(paths, System.getProperty("jdk.module.path"));

        // Check RuntimeMXBean ClassPath
        addPathProperty(paths, ManagementFactory.getRuntimeMXBean().getClassPath());

        // Fallback check URLClassLoader if present
        ClassLoader cl = getClass().getClassLoader();
        if (cl instanceof URLClassLoader urlCl) {
            for (URL url : urlCl.getURLs()) {
                try {
                    paths.add(Path.of(url.toURI()).toAbsolutePath().toString());
                } catch (Exception ignored) {}
            }
        }

        return paths;
    }

    private void addPathProperty(Set<String> set, String prop) {
        if (prop != null && !prop.isBlank()) {
            for (String entry : prop.split(File.pathSeparator)) {
                if (!entry.isBlank()) {
                    set.add(Path.of(entry).toAbsolutePath().toString());
                }
            }
        }
    }
}