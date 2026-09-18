package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MoudlePathTests {
    public static abstract class TestBase {
        public String x;
    }

    public static class TestImpl extends TestBase {
        public String y;
    }

    @Test
    void testModulePath() {
        ModulePath mp = new ModulePath();
        assertTrue(mp.containsClass(TestBase.class.getName()));
    }

}
