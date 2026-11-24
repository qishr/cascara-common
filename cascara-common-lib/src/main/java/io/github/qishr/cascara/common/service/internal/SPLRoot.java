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
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.NonNull;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatConfig;
import io.github.qishr.cascara.common.diagnostic.report.GlobalReporter;
import io.github.qishr.cascara.common.diagnostic.report.ReporterConfig;
import io.github.qishr.cascara.common.i18n.I18nConfig;
import io.github.qishr.cascara.common.security.exception.SignatureException;
import io.github.qishr.cascara.common.security.policy.SPLSignaturePolicy;
import io.github.qishr.cascara.common.security.policy.SignaturePolicy;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.service.ServiceException;
import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.common.util.Config.Setting;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.ContentTypeResolver;
import io.github.qishr.cascara.common.util.JarFile;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.ModulePath.DiscoveredModule;

public class SPLRoot extends SPLBranch {
    // private static final Properties EMPTY_PROPERTIES = new Properties();
    private static final String NL = "\n";

    private static SignaturePolicy signaturePolicy;

    static boolean hasBooted = false;
    static boolean isBooting = false;

    final Set<String> bootProviders = new HashSet<>();

    Map<String,Set<SPL>> moduleToLayers = new HashMap<>();

    private ContentTypeResolver contentTypeStore;
    private Set<ContentType> contentTypes;

    private SPLRoot() {
        isBooting = true;
        rootLayer = this;
        name = "root";
        contentTypes = new HashSet<>();
        moduleLayer = ModuleLayer.boot();
        loadPolicy();
        displayBootInfo();
        if (config.isDiscoveryEnabled()) {
            autoDiscover();
        }
        isBooting = false;
        hasBooted = true;
    }

    /// Retrieves the root Service Provider Layer.
    /// On the initial call, the root layer will be configured.
    @SuppressWarnings("null")
    @NonNull
    public static SPLRoot get() {
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
        return config.getPreferredProvider(serviceType.getName());
    }

    //
    // Private Methods
    //

    private void autoDiscover() {
        discoverClasses();

        try {
            contentTypeStore = SPL.load(ContentTypeResolver.class);
            if (contentTypeStore != null) {
                contentTypeStore.addAll(contentTypes);
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
        REPORTER.debug("Discovering provider candidates");
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

        if (classCount == 0) {
            return;
        }

        REPORTER.debug("Registering providers");
        boolean verify = config.isVerifyEnabled();
        for (DiscoveredModule dm : discovered) {
            Path jarPath = dm.getJarPath();
            if (jarPath != null) {
                try (JarFile jar = JarFile.open(jarPath, verify)) {
                    jar.getManifest();
                    if (verify) {
                        verifySignature(jar);
                    }
                } catch (SignatureException e) {
                    if (config.bootErrorsToConsole()) {
                        System.err.println(e.getMessage());
                    }
                    REPORTER.warn(e);
                    continue;
                } catch (Throwable e) {
                    if (config.bootErrorsToConsole()) {
                        System.err.println(e.getMessage());
                    }
                    REPORTER.warn(e);
                    continue;
                }
            }
            registerProviders(dm, true);
        }

        this.modules.addAll(modules);
    }

    private void collectLayers(SPLBranch layer, Set<SPLBranch> collected) {
        collected.add(layer);
        for (SPLBranch descendant : layer.namedChildren.values()) {
            collectLayers(descendant, collected);
        }
    }

    private void loadPolicy() {
        signaturePolicy = SPLSignaturePolicy.builder()
            .requireSigned(config.policyRequireSigned())
            .checkExpiry(config.policyCheckExpiry())
            .requireSignedClasses(config.policyRequireSignedClasses())
            .build();
    }

    private void displayBootInfo() {
        if (!config.bootInfoToConsole() && !REPORTER.getLevel().includes(Level.DEBUG)) {
            return;
        }

        Writer writer = config.bootInfoToConsole()
            ? new PrintWriter(System.out)
            : REPORTER.getWriter(Level.DEBUG);

        writeBootInfo(writer);
    }

    private void writeBootInfo(Writer writer) {
        try {
            writer.write("Environment Information:" + NL);

            writer.write("  JVM Vendor: " + System.getProperty("java.vendor") + NL);
            writer.write("  JVM Version: " + System.getProperty("java.version") + NL);
            writer.write("  JVM Home: " + System.getProperty("java.home") + NL);
            writer.write("  Operating System: " +
                System.getProperty("os.name") + " " +
                System.getProperty("os.version") + " " +
                System.getProperty("os.arch") + NL
            );

            writer.write("  Cascara Version: " + Cascara.get().getVersion() + NL);
            writer.write("  Module cascara.common Version: " + Cascara.get().getCommonVersion() + NL);

            writer.write("  JPMS Enabled: " + JreUtils.isJpmsEnabled() + NL);
            displayPaths(writer, "Module Path", System.getProperty("jdk.module.path") + NL);
            displayPaths(writer, "Class Path", System.getProperty("java.class.path") + NL);

            writer.write("  Console: " + JreUtils.isRunningInTerminal() + NL);
            // REPORTER.debug("  Eclipse: " + JreUtils.isRunningViaEclipse());
            // REPORTER.debug("  Gradle: " + JreUtils.isRunningViaGradle());

            writer.write("Cascara Config:" + NL);
            for (Setting s : Cascara.get().getAppliedSettings()) {
                writer.write("  " + s + NL);
            }

            writer.write("SPL Config:" + NL);
            for (Setting s : config.getAppliedSettings()) {
                writer.write("  " + s + NL);
            }

            writer.write("Reporter Config:" + NL);
            for (Setting s : ReporterConfig.get().getAppliedSettings()) {
                writer.write("  " + s + NL);
            }

            writer.write("Log Format Config:" + NL);
            for (Setting s : LogFormatConfig.get().getAppliedSettings()) {
                writer.write("  " + s + NL);
            }

            writer.write("Language Config:" + NL);
            for (Setting s : I18nConfig.get().getAppliedSettings()) {
                writer.write("  " + s + NL);
            }

            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayPaths(Writer writer, String name, String paths) throws IOException {
        String[] array = paths == null
            ? new String[]{}
            : paths.split(File.pathSeparator, -1);
        if (array.length == 0) {
            writer.write("  No " + name + NL);
        } else {
            writer.write("  " + name + ":" + NL);
            for (int i = 0; i < array.length; i++) {
                if (i > 0) {
                    writer.write(NL);
                }
                writer.write("    " + array[i]);
            }
        }
    }
}
