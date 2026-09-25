package io.github.qishr.cascara.common.test.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.github.qishr.cascara.common.semver.SemVer;
import io.github.qishr.cascara.common.util.Cascara;

public class EnvironmentAwareTests {
    static boolean isModularEnvironment() {
        return EnvironmentAwareTests.class.getModule().isNamed();
    }

    @Test
    @EnabledIf("isModularEnvironment")
    void test_modularSpecificBehavior() {
        SemVer version = Cascara.getVersion();
        System.out.println("cascara version = " + version);

        Module module = getClass().getModule();
        assertTrue(module.isNamed());
        assertEquals("cascara.common.test", module.getName());
    }

    @Test
    void test_commonBehaviorWorkOnBoth() {
        // Runs regardless of environment
        SemVer version = Cascara.getVersion();
        System.out.println("cascara version = " + version);
        System.out.println("module name = " + getClass().getModule().getName());
    }
}
