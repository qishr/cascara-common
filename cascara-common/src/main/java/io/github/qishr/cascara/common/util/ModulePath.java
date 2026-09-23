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
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.module.ModuleDescriptor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;

@Experimental
public class ModulePath {
    private static final String DOT_CLASS = ".class";
    private static final String DOT_JAR = ".jar";
    private static final String MODULE_INFO = "module-info";

    Set<String> moduleNames = new HashSet<>();
    Set<String> classNames = new HashSet<>();
    Map<String, String> classToModule = new HashMap<>();
    Map<String, Set<String>> moduleToClasses = new HashMap<>();
    Map<String, ModuleDescriptor> descriptors = new HashMap<>();
    Map<String, Path> moduleToPath = new HashMap<>();
    Path virtualReferencePath;

    public ModulePath(String modulePath) {
        loadModulePath(modulePath);
    }

    public ModulePath(String modulePath, Path virtualReferencePath) {
        this.virtualReferencePath = virtualReferencePath;
        loadModulePath(modulePath);
    }

    public ModulePath() {
        // 1. Load standard module path entries
        loadModulePath(System.getProperty("jdk.module.path"));

        // 2. Load patched test module directories from JVM args
        loadPatchedModules();
    }

    public Set<String> getModules() {
        return moduleNames;
    }

    public Set<String> getClasses() {
        return classNames;
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

    public Set<String> getClasses(String moduleName) {
        return moduleToClasses.get(moduleName);
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

    private void parsePatchModuleOption(String option) {
        // Format: <module-name>=<path1>(:<path2>)*
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

    private void loadModulePath(String modulePath) {
        moduleNames = new HashSet<>();
        classToModule = new HashMap<>();
        moduleToClasses = new HashMap<>();

        if (modulePath == null || modulePath.isBlank()) {
            return;
        }

        // Support both system path separator and ':'
        String regexDelimiter = File.pathSeparator.equals(":") ? ":" : "[:" + File.pathSeparator + "]";
        String[] pathList = modulePath.split(regexDelimiter);

        for (String modulePathString : pathList) {
            if (modulePathString.isBlank()) continue;

            // Parse path using virtual file system if provided, otherwise default OS file system
            Path path = (virtualReferencePath != null)
                    ? virtualReferencePath.getFileSystem().getPath(modulePathString)
                    : Path.of(modulePathString);

            if (!Files.exists(path)) {
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

    private String scanJar(Path path, String moduleName) {
        try{
            JarFile jar = JarFile.open(path);
            String jarModuleName = jar.getModuleName();
            Set<String> classNames = jar.getClassNames();
            if (classNames == null) {
                return null;
            }
            for (String className : classNames) {
                addClassToModule(className, jarModuleName);
            }
            if (jarModuleName == null) {
                return moduleName;
            }
            moduleToPath.put(jarModuleName, path);
            return jarModuleName;
        } catch(LocalizableIOException e) {
            // Ignore it
            return null;
        }
    }

    private void addClassToModule(String className, String moduleName) {
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

    private String scanDirectory(Path directory, String moduleName) {
        try {
            URL url = URL.of(directory.toUri(), null);
            URL[] urls = new URL[] {url};
            scanDirectoryUrl(directory, urls, moduleName);
        }catch(MalformedURLException e) {
            // Ignore it
        }
        return moduleName;
    }

    private String scanDirectoryUrl(Path directory, URL[] urls, String moduleName) {
        Set<String> discovered = new HashSet<>();
        try (URLClassLoader classLoader = new URLClassLoader(urls)) {
            try(Stream<Path> classFiles = Files.walk(directory)) {
                for (Path classFile : classFiles.toList()) {
                    if (classFile.toString().endsWith(DOT_CLASS)) {
                        moduleName = scanClassFile(directory, classFile, classLoader, moduleName, discovered);
                    }
                }
                for (String className : discovered) {
                    addClassToModule(className, moduleName);
                }
            }
            try(Stream<Path> jarFiles = Files.walk(directory)) {
                for (Path jarFile : jarFiles.toList()) {
                    if (jarFile.toString().endsWith(DOT_JAR)) {
                        moduleName = scanJar(jarFile, moduleName);
                    }
                }
                for (String className : discovered) {
                    addClassToModule(className, moduleName);
                }
            }
        } catch (IOException e) {
            // Ignore it
        }
        return moduleName;
    }

    private String scanClassFile(Path directory, Path file, URLClassLoader classLoader, String moduleName, Set<String> discovered) throws LocalizableIOException {
        String className = "";
        String relativePath = "";
        try {
            relativePath = directory.toUri().relativize(file.toUri()).getPath();
            className = relativePath.replace(File.separatorChar, '.').replace(DOT_CLASS, "");
            if (className.equals(MODULE_INFO)) {
                ModuleDescriptor descriptor;
                try (InputStream is = Files.newInputStream(file)) {
                    descriptor = ModuleDescriptor.read(is);
                }
                moduleName = descriptor.name();
                descriptors.put(moduleName, descriptor);
                moduleToPath.put(moduleName, file.getParent());
            } else {
                Class<?> clazz = classLoader.loadClass(className);
                for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
                    discovered.add(declaredClass.getName());
                }
            }
        } catch (java.lang.NoClassDefFoundError e) {
            // ctx.error("null, Class not found: " + className + "\n" + e.getMessage());
        } catch (Exception e) {
            // ctx.error(null, "Failed to read class file: " + file);
        }
        return moduleName;
    }
}
