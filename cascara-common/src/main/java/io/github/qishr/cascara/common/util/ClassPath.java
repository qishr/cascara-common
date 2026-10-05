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
import java.lang.module.ModuleDescriptor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;

public class ClassPath extends AbstractLibraryScanner {
    // private static final Reporter REPORTER = GlobalReporter.forClass(ClassPath.class);
    private static final String PATHS = System.getProperty("java.class.path");

    public ClassPath() {
        this(System.getProperty("java.class.path"));
    }

    public ClassPath(String classPath) {
        this(classPath, null);
    }

    public ClassPath(String classPath, Path virtualReferencePath) {
        this(pathSet(classPath), null, virtualReferencePath);
    }

    public ClassPath(Set<String> classPaths, Path virtualReferencePath) {
        this(classPaths, null, virtualReferencePath);
    }

    public ClassPath(Set<String> classPaths, Set<String> excluded) {
        this(classPaths, excluded, null);
    }

    public ClassPath(Set<String> classPaths, Set<String> excluded, Path virtualReferencePath) {
        this.excluded = excluded;
        this.virtualReferencePath = virtualReferencePath;
        loadClassPath(classPaths);
    }

    // @Override
    // protected Reporter getReporter() {
    //     return REPORTER;
    // }

    @Override
    protected void addModuleDescriptor(String moduleName, ModuleDescriptor descriptor) {
    }

    @Override
    protected void addModuleLocation(String moduleName, Path path) {
    }

    @Override
    protected void addClassToModule(String className, String moduleName) {
    }

    private void loadClassPath(Set<String> classPaths) {
        if (classPaths == null || classPaths.isEmpty()) {
            classPaths = new HashSet<>();
            if (PATHS != null) {
                Collections.addAll(classPaths, PATHS.split(File.pathSeparator, -1));
            }
        }

        for (String pathString : classPaths) {
            if (pathString.isBlank()) continue;

            // Parse path using virtual file system if provided, otherwise default OS file system
            Path path = (virtualReferencePath != null)
                    ? virtualReferencePath.getFileSystem().getPath(pathString)
                    : Path.of(pathString);

            if (!Files.exists(path)) {
                // REPORTER.debug("Non-existant class path: " + path);
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

    protected String scanDirectoryUrl(Path directory, URL[] urls, String moduleName) {
        try (URLClassLoader classLoader = new URLClassLoader(urls)) {
            try(Stream<Path> classFiles = Files.walk(directory)) {
                for (Path classFile : classFiles.toList()) {
                    if (classFile.toString().endsWith(DOT_CLASS)) {
                        String relativePath = directory.toUri().relativize(classFile.toUri()).getPath();
                        String className = relativePath.replace(File.separatorChar, '.').replace(DOT_CLASS, "");
                        if (!isExcluded(className)) {
                            addClass(className);
                        }
                    }
                }
            }
        } catch (IOException e) {
            error(e);
        }
        return moduleName;
    }
}
