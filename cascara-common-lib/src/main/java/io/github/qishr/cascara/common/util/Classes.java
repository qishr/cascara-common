package io.github.qishr.cascara.common.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.NonNull;

@Experimental
public class Classes {

    private static final AtomicReference<Set<String>> CLASSES_REF = new AtomicReference<>();
    private static Instant timestamp = Instant.now();

    // TODO Thread safety
    private static Map<String,Path> classLocations = new HashMap<>();
    private static ModulePath modulePath;

    private static final Object LOCK = new Object();

    private Classes (){}

    public static void invalidate() {
        synchronized (LOCK) {
            CLASSES_REF.set(null);
            classLocations.clear();
        }
    }

    private static Set<String> ensureClasses() {
        Set<String> set = CLASSES_REF.get();
        if (set == null) {
            synchronized (LOCK) {
                set = CLASSES_REF.get();
                if (set == null) {
                    Set<String> newSet = new ConcurrentSkipListSet<>();
                    modulePath = new ModulePath();
                    scanSystemPackages(newSet);
                    scanClassPath(newSet);
                    scanModulePath(newSet);
                    CLASSES_REF.set(newSet);
                    set = newSet;
                    timestamp = Instant.now();
                    modulePath = new ModulePath();
                }
            }
        }
        return set;
    }

    @SuppressWarnings("null")
    @NonNull
    public static Instant getTimestamp() {
        return timestamp;
    }

    public static Set<String> getAll() {
        return Collections.unmodifiableSet(ensureClasses());
    }

    public static Path getJarPath(String className) {
        ensureClasses();
        return classLocations.get(className);
    }

    //
    // Private methods
    //

    private static void scanSystemPackages(Set<String> classes) {
        for (String packageName : jrtList("/packages")) {
            scanSystemPackage(packageName, classes);
        }
    }

    private static void scanSystemPackage(String packageName, Set<String> classes) {
        classes.addAll(jrtList("/packages/"+packageName));
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

    private static void scanModulePath(Set<String> classes) {
        Set<String> mpClasses = modulePath.getClasses();
        classes.addAll(mpClasses);
        for (String name : mpClasses) {
            String moduleName = modulePath.getModuleForClass(name);
            Path jarPath = modulePath.getPathForModule(moduleName);
            classLocations.put(name, jarPath);
        }
    }

    private static void scanClassPath(Set<String> classNames) {
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

                    // TODO: This should never be needed
                    String moduleName = modulePath.getModuleForClass(fullName);
                    if (moduleName != null) {
                        Path jarPath = modulePath.getPathForModule(moduleName);
                        if (jarPath != null) {
                            classLocations.put(fullName, jarPath);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // Silently ignore unreadable directories
        }
    }

    private static void scanJarFile(Path jarPath, Set<String> classNames) {
        try (JarFile jar = JarFile.open(jarPath)) {
            Set<String> jarClassNames = jar.getClassNames();
            classNames.addAll(jarClassNames);
            for (String name : jarClassNames) {
                classLocations.put(name, jarPath);
            }
        } catch (Exception e) {}
    }

    public static class Annotated {

        // TODO Thread safety

        private static Map<String,Set<String>> annoToClasses = null;
        private static Instant timestamp = Instant.now();

        public static Set<String> with(Class<? extends Annotation> annotation) {
            return with(annotation.getName());
        }

        @SuppressWarnings("null")
        @NonNull
        public static Set<String> with(String annotation) {
            if (annoToClasses == null || timestamp.isBefore(Classes.getTimestamp())) {
                discover();
            }
            Set<String> classes = annoToClasses.get(annotation);
            return classes == null
                ? Set.of()
                : classes;
        }

        private static void discover() {
            Map<String,Set<String>> map = new HashMap<>();
            Set<String> candidates = Classes.getAll();

            for (String className : candidates) {
                try {
                    Class<?> candidate = Class.forName(className);
                    Annotation[] annos = candidate.getAnnotations();
                    for (Annotation anno : annos) {
                        String annoName = anno.annotationType().getName();
                        Set<String> classes = map.get(annoName);
                        if (classes == null) {
                            classes = new HashSet<>();
                            map.put(annoName, classes);
                        }
                        classes.add(className);
                        Path jarPath = Classes.getJarPath(className);
                        if (jarPath != null) {
                            classLocations.put(className, jarPath);
                        }
                    }
                } catch(ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError | IllegalAccessError e) {

                }
            }
            annoToClasses = map;
            timestamp = Instant.now();
        }
    }
}
