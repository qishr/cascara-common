package integration.test.util;

import java.util.Map;

public interface SourceCode {
    default Map<String, String> sourceCodeA() {
        return Map.of(
            "module-info",
            "module provider.a { " +
            "    requires cascara.common; " +
            "    requires integration.test.fixtures; " +
            "    exports com.example.providera; " +
            "    opens com.example.providera to cascara.common; " +
            "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
            "        with com.example.providera.ProviderA;" +
            "}",

            "com.example.providera.ProviderA",
            "package com.example.providera; " +
            "import integration.test.fixtures.TestService; " +
            "import io.github.qishr.cascara.common.property.Properties; " +
            "public class ProviderA implements TestService { " +
            "    public String getName() { return \"ProviderA\"; } " +
            "    public Properties getServiceProperties() { return null; } " +
            "}"
        );
    }

    default Map<String, String> sourceCodeB() {
        return Map.of(
            "module-info",
            "module provider.b { " +
            "    requires cascara.common; " +
            "    requires integration.test.fixtures; " +
            "    exports com.example.providerb; " +
            "    opens com.example.providerb to cascara.common; " +
            "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
            "        with com.example.providerb.ProviderB;" +
            "}",

            "com.example.providerb.ProviderB",
            "package com.example.providerb; " +
            "import integration.test.fixtures.TestService; " +
            "import io.github.qishr.cascara.common.property.Properties; " +
            "public class ProviderB implements TestService { " +
            "    public String getName() { return \"ProviderB\"; } " +
            "    public Properties getServiceProperties() { return null; } " +
            "}"
        );
    }

    default Map<String, String> sourceCodeS() {
        return Map.of(
            "module-info",
            "module singleton.demo { " +
            "    requires cascara.common; " +
            "    requires integration.test.fixtures; " +
            "    exports com.example.singleton; " +
            "    opens com.example.singleton to cascara.common; " +
            "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
            "        with com.example.singleton.DemoSingletonImpl;" +
            "}",

            "com.example.singleton.DemoSingletonImpl",
            "package com.example.singleton; " +
            "import java.util.UUID; " +
            "import integration.test.fixtures.DemoSingleton; " +
            "import io.github.qishr.cascara.common.annotation.SingletonInitializer; " +
            "public final class DemoSingletonImpl implements DemoSingleton { " +
            "    public static int initCount = 0; " +
            "    public UUID uuid; " +
            "    @SingletonInitializer private void init() { initCount++; uuid = UUID.randomUUID(); } " +
            "    public int getInitCount() { return initCount; } " +
            "    public UUID getUuid() { return uuid; } " +
            "}"
        );
    }


}
