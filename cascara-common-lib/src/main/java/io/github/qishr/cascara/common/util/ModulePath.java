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

package io.github.qishr.cascara.common.util;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.module.ModuleDescriptor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;

@Experimental
public class ModulePath extends AbstractClassFinder {
    private static final String PATHS = System.getProperty("jdk.module.path");

    public ModulePath(Reporter reporter) {
        this(reporter, null, null, null);
        loadPatchedModules();
    }

    public ModulePath() {
        this(null, null, null, null);
        loadPatchedModules();
    }

    public ModulePath(Path virtualReferencePath) {
        this(null, null, null, virtualReferencePath);
        loadPatchedModules();
    }

    public ModulePath(String modulePath) {
        this(null, pathSet(modulePath), null, null);
    }

    public ModulePath(String modulePath, Path virtualReferencePath) {
        this(null, pathSet(modulePath), null, virtualReferencePath);
    }

    public ModulePath(Set<String> modulePaths, Path virtualReferencePath) {
        this(null, modulePaths, null, virtualReferencePath);
    }

    public ModulePath(Set<String> modulePaths, Set<String> excluded, Path virtualReferencePath) {
        this(null, modulePaths, excluded, virtualReferencePath);
    }

    public ModulePath(Reporter reporter, Set<String> modulePaths, Set<String> excluded, Path virtualReferencePath) {
        if (reporter != null) {
            this.reporter = reporter;
        }
        if (modulePaths == null || modulePaths.isEmpty()) {
            modulePaths = new HashSet<>();
            if (PATHS != null) {
                Collections.addAll(modulePaths, PATHS.split(File.pathSeparator, -1));
            }
        }
        this.excluded = excluded;
        this.virtualReferencePath = virtualReferencePath;
        loadModulePath(modulePaths);
    }

    public Set<String> getModuleNames() {
        // return moduleNames;
        return discoveredModules.keySet()
            .stream()
            .filter(s -> !s.equals("UNNAMED"))
            .collect(Collectors.toSet());
    }

    public ModuleDescriptor getDescriptor(String moduleName) {
        return descriptors.get(moduleName);
    }

    public Path getPathForModule(String moduleName) {
        DiscoveredModule mod;
        if (moduleName == null) {
            mod = discoveredModules.get(DiscoveredModule.UNNAMED);
        } else {
            mod = discoveredModules.get(moduleName);
        }
        if (mod == null) {
            throw new IllegalArgumentException("No module named " + moduleName);
        }
        return mod.getJarPath();
    }

    public String getModuleForClass(String className) {
        return classToModule.get(className);
    }

    public boolean containsModule(String moduleName) {
        return discoveredModules.keySet().contains(moduleName);
        // return moduleNames.contains(moduleName);
    }

    //
    // Private Methods
    //

    protected String scanDirectoryUrl(Path directory, URL[] urls, String moduleName) {
        // This is called from ModulePathTests
        Set<String> discovered = new HashSet<>();
        try (URLClassLoader classLoader = new URLClassLoader(urls)) {
            try(Stream<Path> classFiles = Files.walk(directory)) {
                for (Path classFile : classFiles.toList()) {
                    if (classFile.toString().endsWith(DOT_CLASS)) {
                        moduleName = scanClassFile(directory, classFile, classLoader, moduleName, discovered);
                    }
                }
                register(discovered, moduleName, null);
            }
        } catch (IOException e) {
            error(e);
        }
        return moduleName;
    }

    private void loadPatchedModules() {
        reporter.info(GenericMessage.INFO, "Scanning patched modules");
        List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (int i = 0; i < inputArgs.size(); i++) {
            String arg = inputArgs.get(i);
            String patchValue = null;

            if (arg.startsWith("--patch-module=")) {
                patchValue = arg.substring("--patch-module=".length());
            } else if (arg.equals("--patch-module") && i + 1 < inputArgs.size()) {
                patchValue = inputArgs.get(++i);
            }

            if (patchValue != null) {
                parsePatchModuleOption(patchValue);
            }
        }
    }

    private void parsePatchModuleOption(String option) {
        int equalsIdx = option.indexOf('=');
        if (equalsIdx == -1) return;

        String moduleName = option.substring(0, equalsIdx);
        String pathsString = option.substring(equalsIdx + 1);

        reporter.info(GenericMessage.INFO, "Patch: " + moduleName + " = " + pathsString);

        String[] paths = pathsString.split(File.pathSeparator);
        for (String pathStr : paths) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path) && Files.isDirectory(path)) {
                // Scan test directory and append classes to target module
                scanPatchedDirectory(path, moduleName);
            }
        }
    }

    private void scanPatchedDirectory(Path directory, String targetModuleName) {
        Set<String> discovered = new HashSet<>();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{ directory.toUri().toURL() })) {
            try (Stream<Path> classFiles = Files.walk(directory)) {
                for (Path classFile : classFiles.toList()) {
                    if (classFile.toString().endsWith(DOT_CLASS)) {
                        scanClassFile(directory, classFile, classLoader, targetModuleName, discovered);
                    }
                }
            }
        } catch (IOException ignored) {}

        register(discovered, targetModuleName, null);
    }

    private void loadModulePath(Set<String> modulePaths) {
        classToModule = new HashMap<>();

        if (modulePaths == null || modulePaths.isEmpty()) {
            reporter.warn(GenericMessage.WARN, "No module path");
            return;
        }

        reporter.info(GenericMessage.INFO, "Scanning module path");

        int size = modulePaths.size();
        int n = 0;
        for (String modulePathString : modulePaths) {
            n++;
            reporter.info(GenericMessage.INFO, n + " of " + size + ": " + modulePathString);
            if (modulePathString.isBlank()) continue;

            // Parse path using virtual file system if provided, otherwise default OS file system
            Path path = getPath(modulePathString);

            if (!Files.exists(path)) {
                reporter.warn(GenericMessage.WARN, "Non-existant module path: " + path);
                continue;
            }

            String moduleName = "";
            if (Files.isDirectory(path)) {
                moduleName = scanDirectory(path, moduleName);
            } else if (path.toString().toLowerCase().endsWith(DOT_JAR)) {
                moduleName = scanJar(path, moduleName);
            }
        }
    }

    public static class DiscoveredModule {
        public static final String UNNAMED = "UNNAMED";

        private final String name;
        private final Set<String> classes;
        private final Path jarPath;

        public DiscoveredModule(String name, Set<String> classes, Path jarPath) {
            this.name = name;
            this.classes = classes;
            this.jarPath = jarPath;
        }

        public String getName() {
            return name;
        }

        public Set<String> getClasses() {
            return classes;
        }

        public Path getJarPath() {
            return jarPath;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DiscoveredModule other)) {
                return false;
            }
            return name.equals(other.name);
        }
    }
}
