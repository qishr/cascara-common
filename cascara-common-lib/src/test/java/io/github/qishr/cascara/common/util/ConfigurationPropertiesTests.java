package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.annotation.ConfigurationProperty;
import io.github.qishr.cascara.common.annotation.PropertiesNamespace;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class ConfigurationPropertiesTests {

    @PropertiesNamespace("cascara.test")
    public static class TestConfiguration extends ConfigurationProperties {
        // @ConfigurationProperty(name = "cascara.test.level.*")
        @ConfigurationProperty(name = "level")
        private Map<String,Level> levels = new HashMap<>();

        @ConfigurationProperty
        private boolean autoFlush = false;

        @ConfigurationProperty
        private Level level = Level.INFO;

        public TestConfiguration() {}

        public Level getLevel(String fqcn) {
            return getMapValue("levels", Level.class, fqcn);
        }

        public Map<String,Level> getLevels() {
            return getMap("levels", Level.class);
        }

        public boolean isAutoFlush() {
            return getBoolean("autoFlush");
        }

        public Level getLevel() {
            return (Level) getValue("level");
        }
    }

    @Test
    void test_boolean() {
        TestConfiguration cfg = new TestConfiguration();
        assertFalse(cfg.isAutoFlush());

        System.setProperty("cascara.test.autoFlush", "true");
        assertTrue(cfg.isAutoFlush());
    }

    @Test
    void test_enum() {
        TestConfiguration cfg = new TestConfiguration();
        assertEquals(Level.INFO, cfg.getLevel());

        System.setProperty("cascara.test.level", "debug");
        assertEquals(Level.DEBUG, cfg.getLevel());
    }

    @Test
    void test_map() {
        System.setProperty("cascara.test.level.com.example.Test", "debug");
        TestConfiguration cfg = new TestConfiguration();
        assertEquals(Level.DEBUG, cfg.getLevel("com.example.Test"));
    }
}
