package io.github.qishr.cascara.common.service.internal;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;

public class SPLModuleScanner {

    private static Reporter reporter = new NoOpReporter();
    public static Reporter getReporter() { return reporter; }
    public static void setReporter(Reporter r) { reporter = r; }

    public static Set<String> findCandidateProviderClasses(Module module) {
        Set<String> classNames = new HashSet<>();
        ModuleDescriptor descriptor = module.getDescriptor();
        if (descriptor == null) {
            return classNames;
        }

        // 1. SPI declarations in module-info.java
        // TODO: When called from registerModule, this has aleady been done
        // Isn't it already done when call from both callers/
        for (ModuleDescriptor.Provides provides : descriptor.provides()) {
            classNames.addAll(provides.providers());
        }

        // 2. Collect exported and opened packages
        Set<String> accessiblePackages = new HashSet<>();
        descriptor.exports().forEach(e -> accessiblePackages.add(e.source()));
        descriptor.opens().forEach(o -> accessiblePackages.add(o.source()));

        if (accessiblePackages.isEmpty()) {
            return classNames;
        }

        // 3. Try native ModuleReader API
        boolean scannedViaModuleReader = false;
        try {
            Optional<ModuleReference> mrefOpt = Optional.empty();
            if (module.getLayer() != null) {
                mrefOpt = module.getLayer().configuration().findModule(module.getName())
                        .map(ResolvedModule::reference);
            }
            if (mrefOpt.isEmpty()) {
                mrefOpt = ModuleFinder.ofSystem().find(module.getName());
            }

            if (mrefOpt.isPresent()) {
                ModuleReference mref = mrefOpt.get();
                try (ModuleReader reader = mref.open()) {
                    reader.list().forEach(resource -> {
                        if (resource.endsWith(".class") && !resource.equals("module-info.class")) {
                            int lastSlash = resource.lastIndexOf('/');
                            String pkg = lastSlash > 0 ? resource.substring(0, lastSlash).replace('/', '.') : "";

                            if (accessiblePackages.contains(pkg)) {
                                String className = resource.substring(0, resource.length() - 6).replace('/', '.');
                                if (!className.contains("$")) {
                                    classNames.add(className);
                                }
                            }
                        }
                    });
                    scannedViaModuleReader = true;
                }
            }
        } catch (Throwable t) {
            getReporter().trace("  ModuleReader unavailable for " + module.getName() + " (" + t.getMessage() + "), falling back to ClassLoader resources.");
        }

        // 4. Fallback: ClassLoader package scanning (for IDE/Test patching environments)
        if (!scannedViaModuleReader) {
            scanViaClassLoader(module, accessiblePackages, classNames);
        }

        return classNames;
    }

    public static void scanPackageResources(Module module, String packageName, URL packageUrl, Set<String> classNames) {
        try {
            if ("jar".equals(packageUrl.getProtocol())) {
                JarURLConnection conn = (JarURLConnection) packageUrl.openConnection();
                try (java.util.jar.JarFile jar = conn.getJarFile()) {
                    String packagePath = packageName.replace('.', '/') + "/";
                    jar.stream().forEach(entry -> {
                        String name = entry.getName();
                        if (name.startsWith(packagePath) && name.endsWith(".class") && !entry.isDirectory()) {
                            // Extract class name (e.g. io/github/qishr/SchemaStore.class -> io.github.qishr.SchemaStore)
                            String className = name.substring(0, name.length() - 6).replace('/', '.');
                            // Ignore inner classes unless desired
                            if (!className.contains("$")) {
                                classNames.add(className);
                            }
                        }
                    });
                }
            } else if ("file".equals(packageUrl.getProtocol())) {
                Path packageDir = Path.of(packageUrl.toURI());
                if (Files.exists(packageDir)) {
                    try (var stream = Files.list(packageDir)) { // or Files.walk(packageDir, 1)
                        stream.filter(p -> p.toString().endsWith(".class") && !Files.isDirectory(p))
                            .forEach(p -> {
                                String fileName = p.getFileName().toString();
                                String simpleName = fileName.substring(0, fileName.length() - 6);
                                if (!simpleName.contains("$")) {
                                    String fqcn = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
                                    getReporter().trace("      Found candidate class: " + fqcn);
                                    classNames.add(fqcn);
                                }
                            });
                    }
                }
            }
        } catch (Exception ignored) {
            // Skip unreadable entries safely
            getReporter().trace("      Unreachable: " + ignored.getMessage());
        }
    }

    //
    // Private Methods
    //

    private static void scanViaClassLoader(Module module, Set<String> accessiblePackages, Set<String> classNames) {
        ClassLoader cl = module.getClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }

        // Inspect all packages declared on the module
        Set<String> packagesToScan = new HashSet<>(accessiblePackages);
        packagesToScan.retainAll(module.getPackages()); // Only scan packages belonging to this module

        for (String pkg : packagesToScan) {
            String resourcePath = pkg.replace('.', '/');
            try {
                var resources = cl.getResources(resourcePath);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    String protocol = url.getProtocol();

                    if ("jar".equals(protocol) || "file".equals(protocol)) {
                        scanPackageResources(module, pkg, url, classNames);
                    }
                }
            } catch (IOException e) {
                getReporter().trace("  Failed scanning package: " + pkg + " (" + e.getMessage() + ")");
            }
        }

        // Fallback if classloader resources returned 0 classes for an IDE-patched module
        if (classNames.isEmpty()) {
            scanModuleLocation(module, packagesToScan, classNames);
        }
    }

    private static void scanModuleLocation(Module module, Set<String> packagesToScan, Set<String> classNames) {
        // Locate class output directories from the system/context classloader
        for (String pkg : packagesToScan) {
            String resourcePath = pkg.replace('.', '/');
            try {
                Enumeration<URL> systemResources = ClassLoader.getSystemResources(resourcePath);
                while (systemResources.hasMoreElements()) {
                    URL url = systemResources.nextElement();
                    scanPackageResources(module, pkg, url, classNames);
                }
            } catch (IOException ignored) {}
        }
    }
}





// private Set<String> findCandidateProviderClasses(Module module) {
    //     Set<String> classNames = new HashSet<>();
    //     ModuleDescriptor descriptor = module.getDescriptor();
    //     if (descriptor == null) {
    //         getReporter().trace("  No module descriptor");
    //         return classNames; // Automatic or unnamed modules
    //     }

    //     // 1. Inspect native SPI declarations in module-info.java (provides ... with ...)
    //     for (ModuleDescriptor.Provides provides : descriptor.provides()) {
    //         classNames.addAll(provides.providers());
    //     }

    //     // 2. Scan exported and opened packages in the module for candidate classes
    //     Set<String> accessiblePackages = new HashSet<>();
    //     descriptor.exports().forEach(e -> accessiblePackages.add(e.source()));
    //     descriptor.opens().forEach(o -> accessiblePackages.add(o.source()));

    //     if (accessiblePackages.isEmpty()) {
    //         getReporter().trace("  No accessible packages");
    //     }

    //     for (String pkg : accessiblePackages) {
    //         getReporter().trace("  Searching package: " + pkg);
    //         String resourcePath = pkg.replace('.', '/');
    //         try {
    //             // Find all .class resources in the exported/opened package
    //             var resources = module.getClassLoader().getResources(resourcePath);
    //             while (resources.hasMoreElements()) {
    //                 var url = resources.nextElement();
    //                 if ("jar".equals(url.getProtocol()) || "file".equals(url.getProtocol())) {
    //                     getReporter().trace("    Scanning: " + url);
    //                     scanPackageResources(module, pkg, url, classNames);
    //                 } else {
    //                     getReporter().trace("    Unexpected protocol: " + url);
    //                 }
    //             }
    //         } catch (IOException ignored) {
    //             // Log or report non-fatal reading issues
    //             getReporter().trace("    Non-fatal: " + ignored.getMessage());
    //         }
    //     }

    //     return classNames;
    // }




    // private void scanViaClassLoader(Module module, Set<String> accessiblePackages, Set<String> classNames) {
        //     ClassLoader cl = module.getClassLoader();
        //     if (cl == null) return;

        //     for (String pkg : accessiblePackages) {
        //         String resourcePath = pkg.replace('.', '/');
        //         try {
        //             var resources = cl.getResources(resourcePath);
        //             while (resources.hasMoreElements()) {
        //                 URL url = resources.nextElement();
        //                 if ("jar".equals(url.getProtocol()) || "file".equals(url.getProtocol())) {
        //                     scanPackageResources(module, pkg, url, classNames);
        //                 }
        //             }
        //         } catch (IOException ignored) {
        //             getReporter().trace("  Failed scanning package resource: " + pkg);
        //         }
        //     }
        // }











        // private Set<String> findCandidateProviderClasses(Module module) {
    //     Set<String> classNames = new HashSet<>();
    //     ModuleDescriptor descriptor = module.getDescriptor();
    //     if (descriptor == null) {
    //         return classNames;
    //     }

    //     // 1. SPI declarations
    //     for (ModuleDescriptor.Provides provides : descriptor.provides()) {
    //         classNames.addAll(provides.providers());
    //     }

    //     // 2. Exported & opened packages
    //     Set<String> accessiblePackages = new HashSet<>();
    //     descriptor.exports().forEach(e -> accessiblePackages.add(e.source()));
    //     descriptor.opens().forEach(o -> accessiblePackages.add(o.source()));

    //     // 3. Scan module contents using ModuleReader API
    //     try {
    //         Optional<ModuleReference> mrefOpt = ModuleFinder.ofSystem().find(module.getName());
    //         if (mrefOpt.isEmpty() && module.getLayer() != null) {
    //             mrefOpt = module.getLayer().configuration().findModule(module.getName())
    //                     .map(ResolvedModule::reference);
    //         }

    //         if (mrefOpt.isPresent()) {
    //             ModuleReference mref = mrefOpt.get();
    //             try (ModuleReader reader = mref.open()) {
    //                 reader.list().forEach(resource -> {
    //                     if (resource.endsWith(".class") && !resource.equals("module-info.class")) {
    //                         int lastSlash = resource.lastIndexOf('/');
    //                         String pkg = lastSlash > 0 ? resource.substring(0, lastSlash).replace('/', '.') : "";

    //                         if (accessiblePackages.contains(pkg)) {
    //                             String className = resource.substring(0, resource.length() - 6).replace('/', '.');
    //                             if (!className.contains("$")) {
    //                                 classNames.add(className);
    //                             }
    //                         }
    //                     }
    //                 });
    //             }
    //         } else {
    //             // Fallback for non-layer runtime setups
    //             scanViaClassLoader(module, accessiblePackages, classNames);
    //         }
    //     } catch (IOException e) {
    //         getReporter().trace("Failed to read module contents for " + module.getName() + ": " + e.getMessage());
    //     }

    //     return classNames;
    // }

    // private void scanViaClassLoader(Module module, Set<String> accessiblePackages, Set<String> classNames) {
    //     ClassLoader cl = module.getClassLoader();
    //     if (cl == null) return;

    //     for (String pkg : accessiblePackages) {
    //         String resourcePath = pkg.replace('.', '/');
    //         try {
    //             var resources = cl.getResources(resourcePath);
    //             while (resources.hasMoreElements()) {
    //                 URL url = resources.nextElement();
    //                 scanPackageResources(module, pkg, url, classNames);
    //             }
    //         } catch (IOException ignored) {}
    //     }
    // }
