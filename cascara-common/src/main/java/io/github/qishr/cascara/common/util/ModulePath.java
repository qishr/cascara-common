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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;

@Experimental
public class ModulePath extends AbstractLibraryScanner {
    // private static final Reporter REPORTER = GlobalReporter.forClass(ModulePath.class);
    private static final String PATHS = System.getProperty("jdk.module.path");

    private Set<String> moduleNames = new HashSet<>();
    private Map<String, String> classToModule = new HashMap<>();
    private Map<String, Set<String>> moduleToClasses = new HashMap<>();
    private Map<String, ModuleDescriptor> descriptors = new HashMap<>();
    private Map<String, Path> moduleToPath = new HashMap<>();

    public ModulePath() {
        this(null, null, null);
        loadPatchedModules();
    }

    public ModulePath(String modulePath) {
        this(modulePath, null);
    }

    public ModulePath(String modulePath, Path virtualReferencePath) {
        this(pathSet(modulePath), null, virtualReferencePath);
    }

    public ModulePath(Set<String> classPaths, Path virtualReferencePath) {
        this(classPaths, null, virtualReferencePath);
    }

    public ModulePath(Set<String> modulePaths, Set<String> excluded, Path virtualReferencePath) {
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

    public Set<String> getModules() {
        return moduleNames;
    }

    public ModuleDescriptor getDescriptor(String moduleName) {
        return descriptors.get(moduleName);
    }

    public Path getPathForModule(String moduleName) {
        return moduleToPath.get(moduleName);
    }

    public String getModuleForClass(String className) {
        return classToModule.get(className);
    }

    public boolean containsModule(String moduleName) {
        return moduleNames.contains(moduleName);
    }

    public boolean containsClass(String className) {
        return classNames.contains(className);
    }

    //
    // Private Methods
    //

    private void loadPatchedModules() {
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

    // @Override
    // protected Reporter getReporter() {
    //     return REPORTER;
    // }

    protected String scanDirectoryUrl(Path directory, URL[] urls, String moduleName) {
        Set<String> discovered = new HashSet<>();
        try (URLClassLoader classLoader = new URLClassLoader(urls)) {
            try(Stream<Path> classFiles = Files.walk(directory)) {
                for (Path classFile : classFiles.toList()) {
                    if (classFile.toString().endsWith(DOT_CLASS)) {
                        moduleName = scanClassFile(directory, classFile, classLoader, moduleName, discovered);
                    }
                }
                for (String className : discovered) {
                    if (!isExcluded(className)) {
                        addClassToModule(className, moduleName);
                        addClass(className);
                    }
                }
            }
        } catch (IOException e) {
            error(e);
        }
        return moduleName;
    }

    private void parsePatchModuleOption(String option) {
        int equalsIdx = option.indexOf('=');
        if (equalsIdx == -1) return;

        String moduleName = option.substring(0, equalsIdx);
        String pathsString = option.substring(equalsIdx + 1);

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

        for (String className : discovered) {
            addClassToModule(className, targetModuleName);
        }
    }

    private void loadModulePath(Set<String> modulePaths) {
        moduleNames = new HashSet<>();
        classToModule = new HashMap<>();
        moduleToClasses = new HashMap<>();

        if (modulePaths == null || modulePaths.isEmpty()) {
            // REPORTER.debug("No module path");
            return;
        }

        for (String modulePathString : modulePaths) {
            if (modulePathString.isBlank()) continue;

            // Parse path using virtual file system if provided, otherwise default OS file system
            Path path = (virtualReferencePath != null)
                    ? virtualReferencePath.getFileSystem().getPath(modulePathString)
                    : Path.of(modulePathString);

            if (!Files.exists(path)) {
                // REPORTER.debug("Non-existant module path: " + path);
                continue;
            }

            String moduleName = "";
            if (Files.isDirectory(path)) {
                moduleName = scanDirectory(path, moduleName);
            } else if (path.toString().toLowerCase().endsWith(DOT_JAR)) {
                moduleName = scanJar(path, moduleName);
            }

            if (moduleName != null && !moduleName.isEmpty()) {
                moduleNames.add(moduleName);
            }
        }
    }

    @Override
    protected void addModuleDescriptor(String moduleName, ModuleDescriptor descriptor) {
        descriptors.put(moduleName, descriptor);
    }

    @Override
    protected void addModuleLocation(String moduleName, Path path) {
        moduleToPath.put(moduleName, path);
    }

    @Override
    protected void addClassToModule(String className, String moduleName) {
        if (moduleName == null) {
            moduleName = ""; // UNNAMED modules
        }
        classToModule.put(className, moduleName);
        Set<String> moduleClasses = moduleToClasses.get(moduleName);
        if (moduleClasses == null) {
            moduleClasses = new HashSet<>();
            moduleToClasses.put(moduleName, moduleClasses);
        }
        moduleClasses.add(className);
        classNames.add(className);
    }
}
