package io.github.qishr.cascara.test.common.junit.util;

import java.util.Map;

import io.github.qishr.cascara.test.common.junit.base.TestBase;

public class MetaTestBase extends TestBase {
    protected Map<String, String> sourceCode() {
        return Map.of(
            "module-info",
            "module provider.a { " +
            "    requires cascara.common; " +
            // "    requires integration.test.fixtures; " +
            "    exports com.example.providera; " +
            "    opens com.example.providera to cascara.common; " +
            "    provides io.github.qishr.cascara.common.service.ServiceProvider " +
            "        with com.example.providera.ProviderA;" +
            "}",

            "com.example.providera.ProviderA",
            "package com.example.providera; " +
            // "import integration.test.fixtures.TestService; " +
            "import io.github.qishr.cascara.common.service.ServiceProvider; " +
            "import io.github.qishr.cascara.common.property.Properties; " +
            "public class ProviderA implements ServiceProvider { " +
            "    public String getName() { return \"ProviderA\"; } " +
            "    public Properties getServiceProperties() { return null; } " +
            "}"
        );
    }

}
