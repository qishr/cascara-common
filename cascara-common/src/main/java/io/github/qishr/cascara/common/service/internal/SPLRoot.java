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

package io.github.qishr.cascara.common.service.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.filewatcher.FileWatcher;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ClassPath;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.ContentTypeResolver;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.ModulePath;

public class SPLRoot extends SPLBranch {
    private static final Properties EMPTY_PROPERTIES = new Properties();

    final Set<String> bootProviders = new HashSet<>();

    Map<String,Set<SPL>> moduleToLayers = new HashMap<>();

    private ContentTypeResolver contentTypeStore;
    private Set<ContentType> contentTypes;

    private FileWatcher propsFileWatcher;
    private Properties properties;

    private SPLRoot() {
        isBooting = true;
        rootLayer = this;
        name = "root";
        contentTypes = new HashSet<>();
        loadPreferences();
        displayEnvironmentInformation();
        REPORTER.debug("Discovering providers");
        discoverClasses();
        isBooting = false;

        try {
            contentTypeStore = SPL.load(ContentTypeResolver.class);
            // TODO: use addAll
            if (contentTypeStore != null) {
                for (ContentType contentType : contentTypes) {
                    contentTypeStore.add(contentType);
                }
            }
        } catch (ServiceException e) {
            // Ignore
        }
    }

    private void discoverClasses() {
        Set<String> excludePackages = new HashSet<>();
        excludePackages.add("net.bytebuddy");
        excludePackages.add("org.mockito");
        excludePackages.add("org.junit");
        excludePackages.add("org.objenesis");
        excludePackages.add("org.opentest4j");
        excludePackages.add("worker.org.gradle");

        Set<String> classes = new HashSet<>();
        long start = System.currentTimeMillis();

        // Discover classes on the module path
        ModulePath modulePath = new ModulePath(null, excludePackages, null);
        classes.addAll(modulePath.getClasses());

        // Get the set of modulepath entries
        Set<String> modulePathSet = pathSet(System.getProperty("jdk.module.path"));

        // Get the set of classpath entries
        Set<String> classPathSet = pathSet(System.getProperty("java.class.path"));

        // Remove entries from the classpath set that are covered by the modulepath
        classPathSet.removeAll(modulePathSet);

        // Discover classes in the abridged classpath set
        ClassPath classPath = new ClassPath(classPathSet, excludePackages);
        classes.addAll(classPath.getClasses());

        if (REPORTER.getLevel().includes(Level.TRACE)) {
            REPORTER.trace("classes: ");
            classes.stream().sorted().forEach(s -> REPORTER.trace("  " + s));
        }

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        REPORTER.debug("Discovered %d classes in % ms", classes.size(), timeElapsed);

        enumerateProviders(classes, null, null, true);

        modules.addAll(modulePath.getModules());
    }

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured.
    public static SPLRoot instance() {
        if (rootLayer == null) {
            rootLayer = new SPLRoot();
        }
        return rootLayer;
    }

    public Set<ContentType> getContentTypes() {
        return contentTypes;
    }

    public void storeContentType(ContentType contentType) {
        contentTypes.add(contentType);
        if (!isBooting && contentTypeStore != null) {
            contentTypeStore.add(contentType);
        }
    }

    public Set<SPLBranch> allLayers() {
        Set<SPLBranch> collected = new HashSet<>();
        collectLayers(this, collected);
        return collected;
    }

    public String getPreferredProviderClassName(Class<?> serviceType) {
        return getProperties().getString(serviceType.getName());
    }

    public Properties getProperties() {
        Path propsFile = Cascara.getSplPropertiesPath();

        if (!Cascara.isFileTimeSupported()) {
            if (Files.exists(propsFile)) {
                try {
                    properties = Properties.load(propsFile);
                } catch (LocalizableIOException e) {
                    // Ignore and fall through to EMPTY_PROPERTIES
                }
            }
        }

        return properties == null ? EMPTY_PROPERTIES : properties;
    }

    //
    // Private Methods
    //

    private Set<String> pathSet(String paths) {
        Set<String> pathSet = new HashSet<>();
        if (paths != null) {
            Collections.addAll(pathSet, paths.split(File.pathSeparator, -1));
        }
        return pathSet;
    }

    private void collectLayers(SPLBranch layer, Set<SPLBranch> collected) {
        collected.add(layer);
        for (SPLBranch descendant : layer.namedChildren.values()) {
            collectLayers(descendant, collected);
        }
    }

    private void loadPreferences() {
        Path propsFile = Cascara.getSplPropertiesPath();

        if (Cascara.isFileTimeSupported()) {
            if (!Files.exists(propsFile)) {
                try {
                    Files.createFile(propsFile);
                } catch (IOException e) {
                    REPORTER.error(e, FileMessage.WRITE_ERROR, propsFile);
                    return;
                }
            }

            propsFileWatcher = new FileWatcher();
            try {
                propsFileWatcher.watchFile(propsFile, () -> {
                    try {
                        properties = Properties.load(propsFile);
                    } catch (LocalizableIOException e) {}
                });
            } catch (IOException e) {}

            try {
                properties = Properties.load(propsFile);
            } catch (LocalizableIOException e) {}
        }
    }

    private void displayEnvironmentInformation() {
        if (GlobalReporter.globalInstance().getLevel().includes(Level.DEBUG)) {
            REPORTER.debug("Environment Information:");
            REPORTER.debug("  Cascara version: " + Cascara.getVersion());
            REPORTER.debug("  Module cascara.common version: " + Cascara.getCommonVersion());
            REPORTER.debug("  JPMS Enabled: " + JreUtils.isJpmsEnabled());
            displayPaths("Module Path", System.getProperty("jdk.module.path"));
            displayPaths("Class Path", System.getProperty("java.class.path"));
            REPORTER.debug("  Terminal: " + JreUtils.isRunningInTerminal());
            REPORTER.debug("  Eclipse: " + JreUtils.isRunningViaEclipse());
            REPORTER.debug("  Gradle: " + JreUtils.isRunningViaGradle());
        }
    }

    private void displayPaths(String name, String paths) {
        String[] array = paths == null
            ? new String[]{}
            : paths.split(File.pathSeparator, -1);
        if (array.length == 0) {
            REPORTER.debug("  No " + name);
        } else {
            REPORTER.debug("  " + name);
            for (String path : array) {
                REPORTER.debug("    " + path);
            }
        }
    }
}
