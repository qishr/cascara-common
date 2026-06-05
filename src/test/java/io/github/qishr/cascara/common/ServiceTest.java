package io.github.qishr.cascara.common;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.StandardReporter;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.service.ServiceProviderLayer;
import io.github.qishr.cascara.common.service.ServiceMetadata;

public class ServiceTest {
    @Disabled
    @Test
    void t1() {
        Reporter reporter = new StandardReporter().setLevel(Level.DEBUG);
        ServiceProviderLayer root = ServiceProviderLayer.getRootLayer(reporter);
        ServiceProviderLayer layer = root.create("jar-test");
        Path jarPath = Path.of("/Users/sandy/.cascara/extensions/cascara-module-conceptmap-0.1.0.jar");

        layer.registerJar(jarPath);

        for (ServiceMetadata provider : layer.getProvidersByFqcn()) {
            System.out.println(provider.getTitle());
        }
    }
}
