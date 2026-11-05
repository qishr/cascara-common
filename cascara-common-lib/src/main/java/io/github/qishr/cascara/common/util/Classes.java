package io.github.qishr.cascara.common.util;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.NonNull;

@Experimental
public class Classes {

    private static final Set<String> EXCLUDED = Set.of(
        "org.mockito.", "jdk.", "sun.", "com.sun.", "net.bytebuddy.",
        "worker.org.gradle.", "org.objenesis.", "org.opentest4j.",
        "org.jcp.xml.dsig.internal.", "org.junit",
        "com.apple", "apple.laf", "apple.security",
        "java.awt.", "javax.swing."
    );

    private static final AtomicReference<Set<String>> CLASSES_REF = new AtomicReference<>();
    private static Map<String,Path> classLocations = new HashMap<>();
    private static ModulePath modulePath;
    private static ClassPath classPath;
    private static Instant timestamp = Instant.now();
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

                    Path vrp = Cascara.getModulePath();
                    modulePath = new ModulePath(null, null, EXCLUDED, vrp);
                    classPath = new ClassPath(null, EXCLUDED, vrp);

                    scanSystemModules(newSet);
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

    protected static boolean isExcluded(String classOrPkgName) {
        String name = classOrPkgName + ".";
        for (String prefix : EXCLUDED) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void scanSystemModules(Set<String> classes) {
        try {
            FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
            Files.walk(jrt.getPath("/modules")).forEach(p -> {
                if (p.toString().endsWith(".class")) {
                    int count = p.getNameCount();
                    if (count > 3) {
                        String pkgDir = p.subpath(2, count - 1).toString();
                        String pkgName = pkgDir.replace('/', '.');
                        if (!isExcluded(pkgName)) {
                            String clsFileName = p.getFileName().toString();
                            if (!clsFileName.equals("module-info.class")) {
                                String clsName = pkgName + "." + clsFileName.substring(0, clsFileName.length() - 6);
                                classes.add(clsName);
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void scanModulePath(Set<String> classes) {
        Set<String> mpClasses = modulePath.getClasses();
        classes.addAll(mpClasses);
        for (String name : mpClasses) {
            String moduleName = modulePath.getModuleForClass(name);
            if (moduleName != null) {
                Path jarPath = modulePath.getPathForModule(moduleName);
                classLocations.put(name, jarPath);
            }
        }
    }

    private static void scanClassPath(Set<String> classNames) {
        classNames.addAll(classPath.getClasses());
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
                Class<?> candidate;
                try {
                    candidate = Class.forName(className);
                } catch (Throwable t) {
                    error(t, className);
                    continue;
                }
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
            }
            annoToClasses = map;
            timestamp = Instant.now();
        }
    }

    private static void error(Throwable t, String className) {
        // TODO: No system output
        Class<?> tc = t.getClass();
        if (tc != ClassNotFoundException.class && tc != NoClassDefFoundError.class) {
            System.err.println(className + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }

        // Exceptions handled so far:
        // java.lang.Error
        // java.lang.InternalError
        // java.lang.UnsatisfiedLinkError
        // java.lang.ClassNotFoundException
        // java.lang.ExceptionInInitializerError
        // java.lang.NoClassDefFoundError
        // java.lang.IllegalAccessError
    }
}
