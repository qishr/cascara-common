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

import java.io.InputStream;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.code.FileDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;

public class JreUtils {
    /// Returns an `InputStream `for a JRE resource.
    /// @return The `InputStream` returned by `Class.getResourceAsStream`.
    /// @throws LocalizableIOException an exception detailing why the resource was inaccessible.
    public static InputStream getResourceAsStream(Class<?> clazz, String path) throws LocalizableIOException {
        InputStream is = clazz.getResourceAsStream(path);
        if (is == null && path.startsWith("/")) {
            path = path.substring(1);
            is = clazz.getResourceAsStream(path);
        }

        // If clazz is in a JPMS module, check if the module opens the package to the class's module.
        // If it doesn't, put that detail in the exception.
        if (is == null) {
            Module targetModule = clazz.getModule();
            if (targetModule.isNamed()) {
                // Determine the package name of the resource from its path
                String packageName = getPackageNameFromResourcePath(path);

                if (!packageName.isEmpty()) {
                    Module myModule = clazz.getModule();

                    // Check if targetModule does NOT open this package to the class's module
                    if (!targetModule.isOpen(packageName, myModule)) {
                        throw new LocalizableIOException(
                            GenericDiagnosticCode.RESOURCE_INACCESSIBLE,
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
            }
            throw new LocalizableIOException(FileDiagnosticCode.FILE_NOT_FOUND, path);
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

    public static ProcessHandle parentProcess() {
        ProcessHandle ph = ProcessHandle.current();
        return ph.parent().get();
    }

    private static String[] parentProcessArgs() {
        ProcessHandle parent = parentProcess();
        if (parent == null) {
            return null; // Unable to determine
        }
        return parent.info().arguments().get();
    }

    public static boolean isRunningInTerminal() {
        return System.console() != null && System.console().isTerminal();
    }

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
