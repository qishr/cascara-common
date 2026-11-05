package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.service.internal.Discoverer;
import io.github.qishr.cascara.common.type.net.UriDescriptor;
import io.github.qishr.cascara.common.util.ModulePath.DiscoveredModule;

public class DiscovererTests {
    @Test
    void test_discovererDiscoversUriDescriptor() {
        // Pair<Set<String>,Set<String>> discovered = Discoverer.discoverModulesAndClasses(Set.of());
        Set<DiscoveredModule> discovered = Discoverer.discoverModulesAndClasses(Set.of());

        // TODO: Assert this only do this in JPMS environment:
        // Set<String> modules = discovered.getL();
        // assertTrue(modules.contains("cascara.common"), "cascara.common not found in modules");

        boolean found = false;
        for (DiscoveredModule dm : discovered) {
            Set<String> classes = dm.getClasses();
            found |= classes.contains(UriDescriptor.class.getName());
        }
        assertTrue(found, "UriDescriptor not found in classes");
    }
}
