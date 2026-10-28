package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.service.internal.Discoverer;
import io.github.qishr.cascara.common.type.net.UriDescriptor;
import io.github.qishr.cascara.common.util.Pair;

public class DiscovererTests {
    @Test
    void test() {
        Pair<Set<String>,Set<String>> discovered = Discoverer.discoverModulesAndClasses(Set.of());

        // TODO: Assert this only do this in JPMS environment:
        // Set<String> modules = discovered.getL();
        // assertTrue(modules.contains("cascara.common"), "cascara.common not found in modules");

        Set<String> classes = discovered.getR();
        assertTrue(classes.contains(UriDescriptor.class.getName()), "UriDescriptor not found in classes");
    }
}
