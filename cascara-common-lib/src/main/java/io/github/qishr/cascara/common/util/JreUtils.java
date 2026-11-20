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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.i18n.exception.LocalizableIOException;

public class JreUtils {
    public static String getResourceAsString(Class<?> clazz, String path) throws IOException {
        InputStream inputStream = getResourceAsStream(clazz, path);
        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        String content;

        try (BufferedReader reader = new BufferedReader(streamReader)) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[8192];
            int charsRead;

            while ((charsRead = reader.read(buffer)) != -1) {
                result.append(buffer, 0, charsRead);
            }

            content = result.toString();
        } catch (IOException e) {
            throw new LocalizableIOException(e, FileMessage.READ_ERROR, path);
        }
        return content;
    }

    /// Returns an `InputStream` for a JRE resource.
    /// @return The `InputStream` returned by `Class.getResourceAsStream`.
    /// @throws IOException an exception detailing why the resource was inaccessible.
    @Nullable
    public static InputStream getResourceAsStream(Class<?> clazz, String path) throws IOException {
        InputStream is = clazz.getResourceAsStream(path);

        if (is == null) {
            // Fallbacl 1: Try root-relative lookup on the ClassLoader directly
            String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
            ClassLoader cl = clazz.getClassLoader();
            is = (cl != null)
                ? cl.getResourceAsStream(normalizedPath)
                : ClassLoader.getSystemResourceAsStream(normalizedPath);

        }

        // Fallback 2 to Class-relative lookup if needed
        if (is == null) {
            is = clazz.getResourceAsStream(path.startsWith("/") ? path : "/" + path);
        }

        // Fallback 3: GraalVM Native Image package-relative resolution
        if (is == null && !path.startsWith("/")) {
            String pkg = clazz.getPackageName();
            String fullPath = pkg.isEmpty() ? path : pkg.replace('.', '/') + "/" + path;
            is = clazz.getClassLoader().getResourceAsStream(fullPath);
        }

        // If clazz is in a JPMS module, check if the module opens the package to the class's module.
        // If it doesn't, put that detail in the exception.
        // if (is == null) {
        //     // Determine the package name of the resource from its path
        //     String packageName = getPackageNameFromResourcePath(path);

        //     if ("META-INF".equals(packageName)) {
        //         throw new LocalizableIOException(FileMessage.FILE_NOT_FOUND, path);
        //     }

        //     Module targetModule = clazz.getModule();
        //     if (targetModule.isNamed()) {
        //         if (!packageName.isEmpty()) {
        //             Module myModule = JreUtils.class.getModule();

        //             // Check if targetModule does NOT open this package to the class's module
        //             if (!targetModule.isOpen(packageName, myModule)) {
        //                 throw new LocalizableIOException(
        //                     GenericMessage.RESOURCE_INACCESSIBLE,
        //                     path,
        //                     targetModule.getName(),
        //                     packageName,
        //                     myModule.isNamed() ? myModule.getName() : "ALL-UNNAMED",
        //                     packageName,
        //                     myModule.isNamed() ? myModule.getName() : "ALL-UNNAMED",
        //                     targetModule.getName()
        //                 );
        //             }
        //         }
        //     }
        //     throw new LocalizableIOException(FileMessage.FILE_NOT_FOUND, path);
        // }

        if (is == null) {
            // 1. Resolve package name: use clazz.getPackageName() for relative paths,
            //    or extract from path for absolute leading-slash paths.
            String packageName = path.startsWith("/")
                ? getPackageNameFromResourcePath(path)
                : clazz.getPackageName();

            if ("META-INF".equals(packageName)) {
                throw new LocalizableIOException(FileMessage.FILE_NOT_FOUND, path);
            }

            Module targetModule = clazz.getModule();
            if (targetModule.isNamed() && !packageName.isEmpty()) {
                // 2. Compare targetModule (e.g. cascara.ui.demo) against JreUtils's module (cascara.common)
                Module myModule = JreUtils.class.getModule();

                // Check if targetModule does NOT open this package to JreUtils's module
                if (!targetModule.isOpen(packageName, myModule)) {
                    throw new LocalizableIOException(
                        GenericMessage.RESOURCE_INACCESSIBLE,
                        path,
                        targetModule.getName(),
                        packageName,
                        myModule.isNamed() ? myModule.getName() : "ALL-UNNAMED",
                        packageName,
                        myModule.isNamed() ? myModule.getName() : "ALL-UNNAMED",
                        targetModule.getName()
                    );
                }
            }
            throw new LocalizableIOException(FileMessage.FILE_NOT_FOUND, path);
        }

        return is;
    }

    public static String getPackageNameFromResourcePath(String path) {
        // Strip leading slash if present
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;

        int lastSlash = cleanPath.lastIndexOf('/');
        if (lastSlash == -1) {
            // Resource is in the default (unnamed) package
            return "";
        }

        // Extract folder path and replace slashes with dots to make it a package name
        return cleanPath.substring(0, lastSlash).replace('/', '.');
    }

    public static boolean isJpmsEnabled() {
        Module m = JreUtils.class.getModule();
        return m != null && m.getName() != null && !m.getName().isEmpty();
    }

    public static Module getUnnamedModule() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        return cl.getUnnamedModule();
    }

    @Nullable
    public static Module getModule(String moduleName) {
        if (moduleName == null || moduleName.isEmpty()) {
            return getUnnamedModule();
        }
        for (Module module : ModuleLayer.boot().modules()) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public static ProcessHandle parentProcess() {
        ProcessHandle ph = ProcessHandle.current();
        return ph.parent().orElse(null);
    }

    @Nullable
    private static String[] parentProcessArgs() {
        ProcessHandle parent = parentProcess();
        if (parent == null) {
            return null; // Unable to determine
        }
        return parent.info().arguments().orElse(null);
    }

    //
    //
    //

    @Experimental
    public static ClassLoader getEffectiveClassLoader() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return tccl != null ? tccl : ClassLoader.getSystemClassLoader();
    }

    @Experimental
    public static boolean isRunningInTerminal() {
        return System.console() != null;
    }

    @Experimental
    public static boolean isRunningViaGradle() {
        String[] args = parentProcessArgs();
        if (args == null) {
            return false; // Unable to determine
        }
        for (String arg : args) {
            if (arg.contains("gradle-daemon-main") ||
                arg.contains("gradle-instrumentation-agent") ||
                arg.contains("org.gradle.launcher")) {
                return true;
            }
        }
        return false;
    }

    @Experimental
    public static boolean isRunningViaEclipse() {
        // "-Declipse.application=org.eclipse.jdt.ls.core.id1"
        // "-Declipse.product=org.eclipse.jdt.ls.core.product"
        String[] args = parentProcessArgs();
        if (args == null) {
            return false; // Unable to determine
        }
        for (String arg : args) {
            if (arg.contains("-Declipse.application=org.eclipse.jdt.ls.core.id1") ||
                arg.contains("-Declipse.product=org.eclipse.jdt.ls.core.product")) {
                return true;
            }
        }
        return false;
    }
}
