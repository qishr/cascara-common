package io.github.qishr.cascara.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.Reporter;

public abstract class AbstractLibraryScanner {
    protected static final String DOT_CLASS = ".class";
    protected static final String DOT_JAR = ".jar";
    protected static final String MODULE_INFO = "module-info";

    protected abstract Reporter getReporter();
    protected abstract void addModuleDescriptor(String moduleName, ModuleDescriptor descriptor);
    protected abstract void addModuleLocation(String moduleName, Path path);
    protected abstract void addClassToModule(String className, String moduleName);

    protected Set<String> classNames = new HashSet<>();
    protected Path virtualReferencePath;
    protected Set<String> excluded;

    public Set<String> getClasses() {
        return classNames;
    }

    protected static Set<String> pathSet(String classPath) {
        if (classPath == null || classPath.isBlank()) {
            return Set.of();
        }
        String[] pathArray = classPath.split(File.pathSeparator, -1);
        Set<String> classPaths = new HashSet<>();
        Collections.addAll(classPaths, pathArray);
        return classPaths;
    }

    @Nullable
    protected String scanJar(Path path, String moduleName) {
        try{
            JarFile jar = JarFile.open(path);
            String jarModuleName = jar.getModuleName();
            Set<String> jarClassNames = jar.getClassNames();
            if (jarClassNames == null) {
                getReporter().trace("No classes found");
                return null;
            }
            for (String className : jarClassNames) {
                if (!isExcluded(className)) {
                    addClassToModule(className, jarModuleName);
                    addClass(className);
                }
            }
            if (jarModuleName == null) {
                return moduleName;
            }
            addModuleLocation(jarModuleName, path);
            return jarModuleName;
        } catch(LocalizableIOException e) {
            error(e);
            return null;
        }
    }

    protected String scanDirectory(Path directory, String moduleName) {
        try {
            URL url = URL.of(directory.toUri(), null);
            URL[] urls = new URL[] {url};
            scanDirectoryUrl(directory, urls, moduleName);

            try(Stream<Path> jarFiles = Files.walk(directory)) {
                for (Path jarFile : jarFiles.toList()) {
                    if (jarFile.toString().endsWith(DOT_JAR)) {
                        moduleName = scanJar(jarFile, moduleName);
                    }
                }
            }
        } catch (MalformedURLException e) {
            error(e);
        } catch (IOException e) {
            error(e);
        }
        return moduleName;
    }

    //
    //
    //

    protected boolean isExcluded(String className) {
        if (excluded != null) {
            for (String prefix : excluded) {
                if (className.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void addClass(String className) {
        classNames.add(className);
    }

    protected abstract String scanDirectoryUrl(Path directory, URL[] urls, String moduleName);

    protected String scanClassFile(Path directory, Path file, URLClassLoader classLoader, String moduleName, Set<String> discovered) throws LocalizableIOException {
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
                addModuleDescriptor(moduleName, descriptor);
                addModuleLocation(moduleName, file.getParent());
            } else {
                Class<?> clazz = classLoader.loadClass(className);
                for (Class<?> declaredClass : clazz.getDeclaredClasses()) {
                    discovered.add(declaredClass.getName());
                }
            }
        } catch (java.lang.NoClassDefFoundError e) {
            error(e);
        } catch (Exception e) {
            error(e);
        }
        return moduleName;
    }

    protected void error(Throwable t) {
        getReporter().debug(t.getClass().getName() + ": " + t.getMessage());
    }
}
