package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigProperty;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class ConfigTests {

    @ConfigNamespace("cascara.test")
    public static class TestConfig extends AbstractConfig {
        @ConfigProperty(name = "level")
        private Map<String,Level> levels = new HashMap<>();

        @ConfigProperty
        private boolean autoFlush = false;

        @ConfigProperty
        private Level level = Level.INFO;

        public static TestConfig get() {
            return get(TestConfig.class);
        }

        public Level getLevel(String fqcn) {
            return levels.get(fqcn);
        }

        public Map<String,Level> getLevels() {
            return levels;
        }

        public boolean isAutoFlush() {
            return autoFlush;
        }

        public Level getLevel() {
            return level;
        }
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("cascara.test.auto-flush");
        System.clearProperty("cascara.test.level");
        System.clearProperty("cascara.test.level.com.example.Test");
        AbstractConfig.reset();
    }

    @Test
    void test_boolean_systemProperty() {
        System.setProperty("cascara.test.auto-flush", "true");
        TestConfig cfg = TestConfig.get();
        assertTrue(cfg.isAutoFlush());
    }

    @Test
    void test_enum() {
        System.setProperty("cascara.test.level", "debug");
        TestConfig cfg = TestConfig.get();
        assertEquals(Level.DEBUG, cfg.getLevel());
    }

    @Test
    void test_map() {
        System.setProperty("cascara.test.level.com.example.Test", "debug");
        TestConfig cfg = TestConfig.get();
        assertEquals(Level.DEBUG, cfg.getLevel("com.example.Test"));
    }

    @Test
    void test_getLevels() {
        System.setProperty("cascara.test.level.com.example.Test", "debug");
        TestConfig cfg = TestConfig.get();
        Map<String,Level> map = cfg.getLevels();
        assertEquals(Level.DEBUG, map.get("com.example.Test"));
    }
}
