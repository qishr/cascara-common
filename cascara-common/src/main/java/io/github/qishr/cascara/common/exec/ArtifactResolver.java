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

package io.github.qishr.cascara.common.exec;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.List;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.common.util.ReflectionUtils;

@Experimental
public final class ArtifactResolver {

    private ArtifactResolver() {}

    public static Pair<String,String> buildModulePath(List<String> dependencies, String subprojectName) {
        String argModulePath = "";
        String argAddModules = "";
        for (String mavenName : dependencies) {
            Path jarPath = resolveJarPath(mavenName, subprojectName);
            String javaName;
            try (JarFile jarFile = JarFile.open(jarPath)) {
                javaName = jarFile.getModuleName();
            } catch (Exception e) {
                throw new RuntimeException("Failed to open " + jarPath + ": " + e.getMessage());
            }

            if (!argModulePath.isEmpty()) {
                argModulePath += File.pathSeparator;
            }
            argModulePath += jarPath.toString();

            if (!argAddModules.isEmpty()) {
                argAddModules += ",";
            }
            argAddModules += javaName;
        }
        return new Pair<String,String>(argModulePath, argAddModules);
    }

    public static Path getProjectPath() {
        try {
            Pair<Class<?>,String> caller = ReflectionUtils.getCaller(true, true);
            Class<?> callingClass = caller.getL();

            // 1. Get the physical location of the running class file (e.g., bin/test or build/classes/...)
            ProtectionDomain pd = callingClass.getProtectionDomain();
            Path classLocation = Paths.get(pd.getCodeSource().getLocation().toURI()).toAbsolutePath();
            // 2. Climb up the directory tree until reaching the module root folder
            Path current = classLocation;
            Path projectRoot = null;
            while (current != null) {
                Path fileName = current.getFileName();
                if (fileName == null) {
                    throw new RuntimeException("Failed to get project path for class "+classLocation+" - Path has no filename: "+current);
                }
                String dirName = fileName.toString();
                if ("bin".equals(dirName) || "build".equals(dirName) || "out".equals(dirName)) {
                    projectRoot = current.getParent();
                    break;
                }
                current = current.getParent();
            }
            if (projectRoot == null) {
                // Fallback to user.dir if the structure doesn't match standard output folders
                projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            }
            return projectRoot;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to resolve JAR path from class location", e);
        }
    }

    private static boolean dirContains(Path dirPath, String fileName) {
        try {
            for (Path path : Files.list(dirPath).toList()) {
                if (path.getFileName().toString().equals(fileName)) {
                    return true;
                }
            }
        } catch (IOException e) {
        }
        return false;
    }

    public static Path getRootProjectPath() {
        Path rootProjectPath = getProjectPath();
        while (rootProjectPath != null) {
            if (dirContains(rootProjectPath, "settings.gradle")) {
                return rootProjectPath;
            }
            rootProjectPath = rootProjectPath.getParent();
        }
        if (rootProjectPath == null) {
            // Fallback to user.dir if the structure doesn't match standard output folders
            rootProjectPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        }
        return rootProjectPath;
    }

    public static Path getModulepathDir()  {
        return getModulepathDir(null);
    }

    public static Path getModulepathDir(Path projectPath)  {
        Path projectRoot = projectPath == null
            ? getProjectPath()
            : projectPath;
        // 3. Locate build/libs inside the resolved module root
        Path modulepathDir = projectRoot.resolve("build").resolve("modulepath");
        if (!Files.exists(modulepathDir)) {
            throw new IllegalStateException(
                "The 'build/modulepath' directory does not exist at: " + modulepathDir +
                ". Run './gradlew jar' once from the terminal or build the project JAR in your IDE."
            );
        }
        return modulepathDir;
    }

    /// Dynamically locates the compiled module JAR file by traversing up
    /// from the test class's location (handling both bin/ and build/ structures).
    public static Path resolveJarPath(String mavenName) {
        return resolveJarPath(mavenName, null);
    }

    public static Path resolveJarPath(String mavenName, String subprojectName) {
        try {
            Path modulePath;

            if (subprojectName == null) {
                modulePath = getModulepathDir();
            } else {
                modulePath = getModulepathDir(getRootProjectPath().resolve(subprojectName));
            }

            // 4. Find the main target JAR in build/libs
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modulePath, "*.jar")) {
                for (Path jarPath : stream) {
                    String name = jarPath.getFileName().toString();
                    if (match(name, mavenName)) {
                        return jarPath.toAbsolutePath();
                    }
                }
            }
            throw new IllegalStateException("No valid compiled JAR found in: " + modulePath + " for " + mavenName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve JAR path from class location", e);
        }
    }

    private static boolean match(String name, String mavenName) {
        if (mavenName.length() + 4 > name.length()) {
            return false;
        }
        if (!name.startsWith(mavenName)) {
            return false;
        }
        if (!name.endsWith(".jar")) {
            return false;
        }
        String suffix = name.substring(mavenName.length() + 1, name.length() - 4);
        for (int i = 0; i < suffix.length(); i++) {
            char c = suffix.charAt(i);
            if (!((c >= '0' && c <= '9')||c=='.')) {
                return false;
            }
        }
        return true;
    }
}