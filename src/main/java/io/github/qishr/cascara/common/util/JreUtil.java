package io.github.qishr.cascara.common.util;

import java.io.InputStream;

import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.code.FileDiagnosticCode;
import io.github.qishr.cascara.common.diagnostic.code.GenericDiagnosticCode;

public class JreUtil {
    /// Returns an `InputStream `for a JRE resource.
    /// @return The `InputStream` returned by `Class.getResourceAsStream`.
    /// @throws LocalizableIOException an exception detailing why the resource was inaccessible.
    public static InputStream getResourceAsStream(Class<?> clazz, String path) throws LocalizableIOException {
        InputStream is = clazz.getResourceAsStream(path);
        if (is == null && path.startsWith("/")) {
            path = path.substring(1);
            is = clazz.getResourceAsStream(path);
        }

        // If clazz is in a JPMS module, check if the module opens the package to this module.
        // If it doesn't, put that detail in the exception.
        if (is == null) {
            Module targetModule = clazz.getModule();
            if (targetModule.isNamed()) {
                // Determine the package name of the resource from its path
                String packageName = getPackageNameFromResourcePath(path);

                if (!packageName.isEmpty()) {
                    Module myModule = clazz.getModule();

                    // Check if targetModule does NOT open this package to your module
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

}
