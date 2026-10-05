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
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.Nullable;




import java.util.Collections;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Experimental
public class ClassHierarchy {

    private static final AtomicReference<Map<String, Set<String>>> HIERARCHY_REF = new AtomicReference<>();

    private static final Object LOCK = new Object();

    private ClassHierarchy() {}

    public static void invalidate() {
        synchronized (LOCK) {
            HIERARCHY_REF.set(null);
        }
    }

    private static Map<String, Set<String>> ensureHierarchy() {
        Map<String, Set<String>> map = HIERARCHY_REF.get();
        if (map == null) {
            // System.out.println("ensureHierarchy-1");
            synchronized (LOCK) {
                // System.out.println("ensureHierarchy-2");
                map = HIERARCHY_REF.get();
                if (map == null) {
                    // System.out.println("ensureHierarchy-3");
                    Map<String, Set<String>> newMap = new ConcurrentHashMap<>();
                    scanSystemPackages(newMap);
                    // System.out.println("ensureHierarchy-4");
                    scanClassPath(newMap);
                    // System.out.println("ensureHierarchy-5");
                    scanModulePath(newMap);
                    // System.out.println("ensureHierarchy-6");

                    HIERARCHY_REF.set(newMap);
                    map = newMap;
                }
            }
        }
        return map;
    }

    @Nullable
    public static List<String> getSubclasses(String className) {
        Set<String> classes = ensureHierarchy().get(className);
        return classes == null
            ? null
            : classes.stream().sorted().toList();
    }

    public static Map<String, Set<String>> getAll() {
        return Collections.unmodifiableMap(ensureHierarchy());
    }

    private static void save(String k, String v, Map<String, Set<String>> hierarchy) {
        hierarchy.computeIfAbsent(k, key -> ConcurrentHashMap.newKeySet()).add(v);
    }

    //
    // Private methods
    //

    private static void scanSystemPackages(Map<String, Set<String>> hierarchy) {
        for (String packageName : jrtList("/packages")) {
            scanSystemPackage(packageName, hierarchy);
        }
    }

    private static void scanSystemPackage(String packageName, Map<String, Set<String>> hierarchy) {
        for (String className : jrtList("/packages/"+packageName)) {
            List<Class<?>> ancestry = new ArrayList<>();
            Class<?> jvmClass = loadClassSafely(className);
            collectAncestry(jvmClass, ancestry);
            for (Class<?> superType : ancestry) {
                save(superType.getName(), className, hierarchy);
            }
        }
    }

    private static List<String> jrtList(String path) {
        List<String> list = new ArrayList<>();
        FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        Path modulesPath = jrt.getPath(path);
        try (DirectoryStream<Path> entryStream = Files.newDirectoryStream(modulesPath)) {
            for (Path entry : entryStream) {
                list.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            // Ignore it
        }
        return list;
    }

    private static void scanModulePath(Map<String, Set<String>> hierarchy) {
        ModulePath mp = new ModulePath();
        for (String className : mp.getClasses()) {
            // System.out.println("scanModulePath-1 className = " + className);
            Class<?> jvmClass = loadClassSafely(className);
            // System.out.println("scanModulePath-2");
            // System.out.println("scanModulePath jvmClass = " + jvmClass);
            List<Class<?>> ancestry = new ArrayList<>();
            collectAncestry(jvmClass, ancestry);
            for (Class<?> h : ancestry) {
                save(h.getName(), className, hierarchy);
            }
        }
    }

    private static void scanClassPath(Map<String, Set<String>> hierarchy) {
        Set<String> classNames = new HashSet<>();

        // 1. Scan java.class.path entries
        String classpath = System.getProperty("java.class.path");
        if (classpath != null) {
            String[] paths = classpath.split(File.pathSeparator);
            for (String path : paths) {
                Path file = Path.of(path);
                if (!Files.exists(file)) continue;

                try {
                    if (Files.isDirectory(file)) {
                        scanDirectory(file, "", classNames);
                    } else if (file.getFileName().toString().endsWith(".jar")) {
                        scanJarFile(file, classNames);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // 2. Scan TCCL root resource directories
        try {
            ClassLoader cl = JreUtils.getEffectiveClassLoader();
            Enumeration<URL> roots = cl.getResources("");
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                if ("file".equals(root.getProtocol())) {
                    Path dirPath = Path.of(root.toURI());
                    if (Files.isDirectory(dirPath)) {
                        scanDirectory(dirPath, "", classNames);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Load classes and collect hierarchies
        for (String className : classNames) {
            try {
                Class<?> jvmClass = loadClassSafely(className);
                if (jvmClass == null) {
                    continue;
                }

                List<Class<?>> ancestry = new ArrayList<>();
                collectAncestry(jvmClass, ancestry);

                for (Class<?> superType : ancestry) {
                    save(superType.getName(), className, hierarchy);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void scanDirectory(Path fileOrDir, String currentPrefix, Set<String> classNames) {
        if (!Files.isDirectory(fileOrDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileOrDir)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();

                if (Files.isDirectory(path)) {
                    // Keep building package prefix with dots
                    String nextPrefix = currentPrefix.isEmpty() ? fileName : currentPrefix + "." + fileName;
                    scanDirectory(path, nextPrefix, classNames);
                } else if (fileName.endsWith(".class")) {
                    String rawName = fileName.substring(0, fileName.length() - 6);

                    // If top-level file in package directory contains $, preserve binary name format (Outer$Inner)
                    String fullName = currentPrefix.isEmpty() ? rawName : currentPrefix + "." + rawName;
                    classNames.add(fullName);
                }
            }
        } catch (IOException ignored) {
            // Silently ignore unreadable directories
        }
    }

    private static void scanJarFile(Path jarFile, Set<String> classNames) throws Exception {
        try (JarFile jar = JarFile.open(jarFile)) {
            classNames.addAll(jar.getClassNames());
        }
    }

    private static void collectAncestry(Class<?> type, List<Class<?>> collected) {
        if (type == null || type == Object.class) return;

        // Collect all interfaces recursively
        for (Class<?> iface : type.getInterfaces()) {
            collected.add(iface);
            collectAncestry(iface, collected);
        }

        // Collect superclass recursively
        Class<?> superClass = type.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            collected.add(superClass);
            collectAncestry(superClass, collected);
        }
    }

    private static Class<?> loadClassSafely(String className) {
        ClassLoader cl = JreUtils.getEffectiveClassLoader();
        try {
            return Class.forName(className, false, cl);
        } catch (NoClassDefFoundError e) {
        } catch (ClassNotFoundException e) {
        }
        try {
            // Fallback to caller classloader (covers test runner frameworks)
            return Class.forName(className, false, ClassHierarchy.class.getClassLoader());
        } catch (NoClassDefFoundError | ClassNotFoundException e2) {
            return null;
        }
    }
}