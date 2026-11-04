package io.github.qishr.cascara.common.service.internal;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ClassPath;
import io.github.qishr.cascara.common.util.ModulePath;
import io.github.qishr.cascara.common.util.ModulePath.DiscoveredModule;

public class Discoverer {
    public static Set<DiscoveredModule> discoverModulesAndClasses(Set<String> excludePackages) {
        // Discover classes on the module path
        ModulePath modulePath = new ModulePath(null, excludePackages, Cascara.getModulePath());
        Set<DiscoveredModule> modules = modulePath.getDiscoveredModules();

        // Get the set of modulepath entries
        Set<String> modulePathSet = pathSet(System.getProperty("jdk.module.path"));

        // Get the set of classpath entries
        Set<String> classPathSet = pathSet(System.getProperty("java.class.path"));

        // Remove entries from the classpath set that are covered by the modulepath
        classPathSet.removeAll(modulePathSet);

        // Discover classes in the abridged classpath set
        ClassPath classPath = new ClassPath(classPathSet, excludePackages, Cascara.getModulePath());
        Set<DiscoveredModule> cpModules = classPath.getDiscoveredModules();

        modules.addAll(cpModules);

        return modules;
    }

    private static Set<String> pathSet(String paths) {
        Set<String> pathSet = new HashSet<>();
        if (paths != null) {
            Collections.addAll(pathSet, paths.split(File.pathSeparator, -1));
        }
        return pathSet;
    }
}
