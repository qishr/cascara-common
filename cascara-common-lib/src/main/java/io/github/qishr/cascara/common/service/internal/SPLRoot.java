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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableIOException;
import io.github.qishr.cascara.common.diagnostic.message.FileMessage;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.filewatcher.FileWatcher;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.security.SignatureException;
import io.github.qishr.cascara.common.security.SignaturePolicy;
import io.github.qishr.cascara.common.security.SplSignaturePolicy;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.ContentTypeResolver;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.ModulePath.DiscoveredModule;

public class SPLRoot extends SPLBranch {
    private static final Properties EMPTY_PROPERTIES = new Properties();

    private static SignaturePolicy signaturePolicy;

    static boolean hasBooted = false;
    static boolean isBooting = false;

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
        moduleLayer = ModuleLayer.boot();
        loadPolicy();
        loadPreferences();
        displayEnvironmentInformation();
        if (isAutoDiscoverEnabled()) {
            autoDiscover();
        }
        isBooting = false;
        hasBooted = true;
    }

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured.
    @SuppressWarnings("null")
    @NonNull
    public static SPLRoot instance() {
        if (rootLayer == null) {
            rootLayer = new SPLRoot();
        }
        return rootLayer;
    }

    public static boolean hasBooted() {
        return hasBooted;
    }

    public static boolean isBooting() {
        return isBooting;
    }

    public static SignaturePolicy getSignaturePolicy() {
        return signaturePolicy;
    }

    public static void setSignaturePolicy(SignaturePolicy policy) {
        signaturePolicy = policy;
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
        Path propsFile = Cascara.instance().getSplPropertiesPath();

        if (!Cascara.instance().isFileTimeSupported()) {
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

    private void autoDiscover() {
        REPORTER.debug("Discovering providers");
        discoverClasses();

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

        // Set up the GlobalReporter serializer.
        // This is done here rather than in GlobalReporter because GlobalReporter
        // is used by SPL, and only SPL knows when the serializer becomes available.
        GlobalReporter.configureSerializer();
    }

    private void discoverClasses() {
        Set<String> excludePackages = new HashSet<>();
        excludePackages.add("net.bytebuddy");
        excludePackages.add("org.mockito");
        excludePackages.add("org.junit");
        excludePackages.add("org.objenesis");
        excludePackages.add("org.opentest4j");
        excludePackages.add("worker.org.gradle");

        long start = System.currentTimeMillis();

        Set<DiscoveredModule> discovered = Discoverer.discoverModulesAndClasses(excludePackages);

        Set<String> modules = new HashSet<>();
        for (DiscoveredModule dm : discovered) {
            if (dm.getName() != null) {
                modules.add(dm.getName());
            }
        }

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        int classCount = 0;
        for (DiscoveredModule dm : discovered) {
            classCount += dm.getClasses().size();
        }

        REPORTER.debug("Discovered %d classes in %d ms", classCount, (int)timeElapsed);

        boolean verify = isVerifyEnabled();

        for (DiscoveredModule dm : discovered) {
            Path jarPath = dm.getJarPath();
            if (jarPath != null) {
                try (JarFile jar = JarFile.open(jarPath, verify)) {
                    jar.getManifest();
                    if (verify) {
                        verifySignature(jar);
                    }
                } catch (SignatureException e) {
                    if (isLoggingBootErrorsToConsole()) {
                        System.err.println(e.getMessage());
                    }
                    REPORTER.warn(e);
                    continue;
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    continue;
                }
            }
            enumerateProviders(dm, true);
        }

        this.modules.addAll(modules);
    }

    private void collectLayers(SPLBranch layer, Set<SPLBranch> collected) {
        collected.add(layer);
        for (SPLBranch descendant : layer.namedChildren.values()) {
            collectLayers(descendant, collected);
        }
    }

    private boolean isLoggingBootErrorsToConsole() {
        // TODO: Use propsFile for this too
        String b = System.getProperty(CASCARA_SPL_BOOT_ERRORS_TO_CONSOLE);
        return "true".equalsIgnoreCase(b);
    }

    private boolean isAutoDiscoverEnabled() {
        // TODO: Use propsFile for this too
        String autoDiscover = System.getProperty(CASCARA_SPL_DISCOVER);
        return !"false".equalsIgnoreCase(autoDiscover);
    }

    private void loadPolicy() {
        // TODO: Also allow loading from SPL properties file
        // but provide a utility class that handles env vars,
        // properties, and properties files which can be reused
        // by other parts of Cascara.instance(). This should use the
        // policy class's (or whatever other class it's reading
        // settings for) schema to validate the settings.

        // Access system properties through Cascara Properties class
        Properties sysprops = Properties.fromSystemProperties();

        signaturePolicy = SplSignaturePolicy.builder()
            .requireSigned(sysprops.getBoolean(CASCARA_SPL_POLICY_REQUIRE_SIGNED, false))
            .checkExpiry(sysprops.getBoolean(CASCARA_SPL_POLICY_CHECK_EXPIRY, false))
            .build();
    }

    private void loadPreferences() {
        // TODO see above
        Path propsFile = Cascara.instance().getSplPropertiesPath();

        if (Cascara.instance().isFileTimeSupported()) {
            if (!Files.exists(propsFile)) {
                try {
                    Files.createFile(propsFile);
                } catch (IOException e) {
                    // e.printStackTrace();
                    // TODO: Human readable IO exception details...
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
        // TODO: JVM info
        if (REPORTER.getLevel().includes(Level.DEBUG)) {
            REPORTER.debug("Environment Information:");
            REPORTER.debug("  Cascara version: " + Cascara.instance().getVersion());
            REPORTER.debug("  Module cascara.common version: " + Cascara.instance().getCommonVersion());
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
