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

    // private static final Set<String> EXCLUDED = Set.of("org.mockito", "jdk.internal", "jdk.jfr");
    private static final Set<String> EXCLUDED = Set.of(
        "org.mockito.", "jdk.", "sun.", "com.sun.", "net.bytebuddy.",
        "worker.org.gradle.", "org.objenesis.", "org.opentest4j.",
        "org.jcp.xml.dsig.internal.", "org.junit",
        "com.apple", "apple.laf", "apple.security",
        "java.awt.", "javax.swing."
    );

    private static final AtomicReference<Set<String>> CLASSES_REF = new AtomicReference<>();
    private static Instant timestamp = Instant.now();

    // TODO Thread safety
    private static Map<String,Path> classLocations = new HashMap<>();
    private static ModulePath modulePath;
    private static ClassPath classPath;

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

                    // TODO: Make ClassPath exclude packages
                    classPath = new ClassPath(null, EXCLUDED, vrp);

                    // scanSystemPackages(newSet);

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
        if (classOrPkgName.contains("awt")) {
            System.err.println("NOT EXCLUDING: " + classOrPkgName);
        }
        return false;
    }

    private static void scanSystemModules(Set<String> classes) {
        // System.err.println("Debug: scanSystemModules");
        try {
            FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
            // Files.walk(jrt.getPath("/")).forEach(p -> {
            //     System.err.println("PPPP " + p);
            // });
            Files.walk(jrt.getPath("/modules")).forEach(p -> {
                // System.err.println("**** " + p);

                // if (p.toString().contains("java.base")) {
                //     System.err.println("**** " + p);
                // }
                if (p.toString().endsWith(".class")) {
                    int count = p.getNameCount();
                    if (count > 3) {
                        // String modName = p.getName(1).toString();
                        String pkgDir = p.subpath(2, count - 1).toString();
                        String pkgName = pkgDir.replace('/', '.');


                        // if (pkgName.contains("awt")) {
                        //     System.err.println("YYY: " + p);
                        // }

                        if (!isExcluded(pkgName)) {

                            // if (pkgName.contains("awt")) {
                            //     System.err.println("XXX: " + pkgName);
                            // }


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
            if (moduleName == null) {
                // throw new UnexpectedNullReturnException("modulePath", "getModuleForClass");
            } else {
                if (name.contains("bytebuddy")) {
                    System.out.println("ERROR!!!!!!!!!!!!!!!! " + name);
                }
                Path jarPath = modulePath.getPathForModule(moduleName);
                classLocations.put(name, jarPath);
            }
        }
    }

    private static void scanClassPath(Set<String> classNames) {
        Set<String> cpNames = classPath.getClasses();
        classNames.addAll(cpNames);
    }

    // private static void old_scanClassPath(Set<String> classNames) {
    //     // 1. Scan java.class.path entries
    //     String classpath = System.getProperty("java.class.path");
    //     if (classpath != null) {
    //         String[] paths = classpath.split(File.pathSeparator);
    //         for (String path : paths) {
    //             Path file = Path.of(path);
    //             if (!Files.exists(file)) continue;

    //             try {
    //                 if (Files.isDirectory(file)) {
    //                     scanDirectory(file, "", classNames);
    //                 } else if (file.getFileName().toString().endsWith(".jar")) {
    //                     scanJarFile(file, classNames);
    //                 }
    //             } catch (Exception ignored) {
    //             }
    //         }
    //     }

    //     // 2. Scan TCCL (Thread Context Class Loader) root resource directories
    //     try {
    //         // TODO: Do this in ClassPath class
    //         ClassLoader cl = JreUtils.getEffectiveClassLoader();
    //         Enumeration<URL> roots = cl.getResources("");
    //         while (roots.hasMoreElements()) {
    //             URL root = roots.nextElement();
    //             if ("file".equals(root.getProtocol())) {
    //                 Path dirPath = Path.of(root.toURI());
    //                 if (Files.isDirectory(dirPath)) {
    //                     scanDirectory(dirPath, "", classNames);
    //                 }
    //             }
    //         }
    //     } catch (Exception ignored) {
    //     }
    // }

    // private static void scanDirectory(Path fileOrDir, String currentPrefix, Set<String> classNames) {
    //     if (!Files.isDirectory(fileOrDir)) return;

    //     try (DirectoryStream<Path> stream = Files.newDirectoryStream(fileOrDir)) {
    //         for (Path path : stream) {
    //             String fileName = path.getFileName().toString();

    //             if (Files.isDirectory(path)) {
    //                 // Keep building package prefix with dots
    //                 String nextPrefix = currentPrefix.isEmpty() ? fileName : currentPrefix + "." + fileName;
    //                 scanDirectory(path, nextPrefix, classNames);
    //             } else if (fileName.endsWith(".class")) {
    //                 String rawName = fileName.substring(0, fileName.length() - 6);

    //                 // If top-level file in package directory contains $, preserve binary name format (Outer$Inner)
    //                 String fullName = currentPrefix.isEmpty() ? rawName : currentPrefix + "." + rawName;
    //                 classNames.add(fullName);

    //                 // TODO: This should never be needed
    //                 String moduleName = modulePath.getModuleForClass(fullName);
    //                 if (moduleName != null) {
    //                     Path jarPath = modulePath.getPathForModule(moduleName);
    //                     if (jarPath != null) {
    //                         classLocations.put(fullName, jarPath);
    //                     }
    //                 }
    //             }
    //         }
    //     } catch (IOException ignored) {
    //         // Silently ignore unreadable directories
    //     }
    // }

    // private static void scanJarFile(Path jarPath, Set<String> classNames) {
    //     try (JarFile jar = JarFile.open(jarPath)) {
    //         Set<String> jarClassNames = jar.getClassNames();
    //         classNames.addAll(jarClassNames);
    //         for (String name : jarClassNames) {
    //             classLocations.put(name, jarPath);
    //         }
    //     } catch (Exception e) {}
    // }

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
                if (
                //     // Something in here:
                    className.contains("java.awt.") ||
                    className.contains("javax.swing.")



                    // className.contains("github") ||
                    // className.contains("java.reflect") ||
                    // className.contains("java.security") ||
                    // className.contains("java.lang") ||
                    // className.contains("java.net") ||
                    // className.contains("java.io") ||
                    // className.contains("java.nio") ||
                    // className.contains("java.beans") ||
                    // className.contains("java.time") ||
                    // className.contains("java.text") ||
                    // className.contains("java.sql") ||
                    // className.contains("java.math") ||


                    // className.contains("bytebuddy") ||
                    // className.contains("java.applet") ||
                    // className.contains("java.rmi") ||
                    // className.contains("java.util") ||
                    // className.contains("org.junit") ||
                    // className.contains("org.mockito") ||


                    // className.contains("com.apple") ||
                    // className.contains("apple.laf") ||
                    // className.contains("apple.security")


                ) {
                    System.out.println("ERROR: " + className);
                    continue;
                }


                // System.out.println("Checking annotations in " + className);


                // if (className.contains("bytebuddy")) {
                //     System.out.println("BB " + className);
                // }


                Class<?> candidate;
                try {
                    candidate = Class.forName(className);
                } catch (java.lang.UnsatisfiedLinkError | java.lang.InternalError e) {
                    error(e, className);
                    continue;
                } catch(ClassNotFoundException | ExceptionInInitializerError | NoClassDefFoundError | IllegalAccessError e) {
                    error(e, className);
                    continue;
                } catch (java.lang.Error e) {
                    // This would include the following of they weren't specified above:
                    // - UnsatisfiedLinkError
                    // - ExceptionInInitializerError
                    error(e, className);
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
