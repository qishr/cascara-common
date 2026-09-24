package io.github.qishr.cascara.common.test.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.util.Cascara;
import io.github.qishr.cascara.test.common.junit.util.TestModulePackager;
import io.github.qishr.cascara.test.common.junit.util.VfsTestBase;

public class SplTestBase extends VfsTestBase {
        protected Path createModuleA() throws IOException {
        Path providerAJar = Cascara.getModulePath().resolve("provider-a.jar");

        // Synthetic Module A with explicit module-info
        TestModulePackager.createSyntheticModuleJar(
            providerAJar,
            "0.10.0",
            Map.of(
                "module-info",
                "module provider.a { " +
                "    requires cascara.common; " +
                "    requires cascara.test.common.junit; " +
                "    exports com.example.providera; " +
                "    opens com.example.providera to cascara.common; " +
                "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
                "        with com.example.providera.ProviderA;" +
                "}",

                "com.example.providera.ProviderA",
                "package com.example.providera; " +
                "import io.github.qishr.cascara.test.common.junit.service.TestService; " +
                "import io.github.qishr.cascara.common.property.Properties; " +
                "public class ProviderA implements TestService { " +
                "    public String getName() { return \"ProviderA\"; } " +
                "    public Properties getServiceProperties() { return null; } " +
                "}"
            ),
            List.of("cascara-common", "cascara-test-common-junit"),
            "cascara-common-test"
        );
        return providerAJar;
    }

    protected Path createModuleB() throws IOException {
        Path providerBJar = Cascara.getModulePath().resolve("provider-b.jar");
        // Synthetic Module B with explicit module-info
        TestModulePackager.createSyntheticModuleJar(
            providerBJar,
            "0.10.0",
            Map.of(
                "module-info",
                "module provider.b { " +
                "    requires cascara.common; " +
                "    requires cascara.test.common.junit; " +
                "    exports com.example.providerb; " +
                "    opens com.example.providerb to cascara.common; " +
                "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
                "        with com.example.providerb.ProviderB;" +
                "}",

                "com.example.providerb.ProviderB",
                "package com.example.providerb; " +
                "import io.github.qishr.cascara.test.common.junit.service.TestService; " +
                "import io.github.qishr.cascara.common.property.Properties; " +
                "public class ProviderB implements TestService { " +
                "    public String getName() { return \"ProviderB\"; } " +
                "    public Properties getServiceProperties() { return null; } " +
                "}"
            ),
            List.of("cascara-common", "cascara-test-common-junit"),
            "cascara-common-test"
        );
        return providerBJar;
    }

    protected Path createSingletonModule() throws IOException {
        Path jar = Cascara.getModulePath().resolve("singleton.jar");

        TestModulePackager.createSyntheticModuleJar(
            jar,
            "0.10.0",
            Map.of(
                "module-info",
                "module singleton.demo { " +
                "    requires cascara.common; " +
                // "    requires cascara.test.integration; " +
                "    requires cascara.test.common.junit; " +
                "    exports com.example.singleton; " +
                "    opens com.example.singleton to cascara.common; " +
                "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
                "        with com.example.singleton.DemoSingletonImpl;" +
                "}",

                "com.example.singleton.DemoSingletonImpl",
                "package com.example.singleton; " +
                "import java.util.UUID; " +
                "import io.github.qishr.cascara.test.common.junit.service.DemoSingleton; " +
                "import io.github.qishr.cascara.common.annotation.SingletonInitializer; " +
                "public final class DemoSingletonImpl implements DemoSingleton { " +
                "    public static int initCount = 0; " +
                "    public UUID uuid; " +
                "    @SingletonInitializer private void init() { initCount++; uuid = UUID.randomUUID(); } " +
                "    public int getInitCount() { return initCount; } " +
                "    public UUID getUuid() { return uuid; } " +
                "}"
            ),
            List.of("cascara-common", "cascara-test-common-junit"),
            "cascara-common-test"
        );

        return jar;
    }
}
