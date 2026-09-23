package io.github.qishr.cascara.common.test.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.util.ModulePath;

public class MoudlePathTests {
    public static abstract class TestBase {
        public String x;
    }

    public static class TestImpl extends TestBase {
        public String y;
    }

    @Test
    void testModulePath() {
        String mouleName = getClass().getModule().getName();
        if (mouleName == null || mouleName.isEmpty()) {
            System.out.println("Skipping testModulePath in non-JPMS environment");
        } else {
            ModulePath mp = new ModulePath();
            assertTrue(mp.containsClass(TestBase.class.getName()));
        }
    }
}
