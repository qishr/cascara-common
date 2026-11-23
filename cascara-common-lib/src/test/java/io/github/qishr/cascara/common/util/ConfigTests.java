package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import io.github.qishr.cascara.common.annotation.ConfigExtends;
import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.annotation.ConfigSetting;
import io.github.qishr.cascara.common.annotation.ConfigSetting.Op;
import io.github.qishr.cascara.common.annotation.ConfigSetting.Source;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

@Isolated
public class ConfigTests {

    @ConfigNamespace("test")
    public static class BaseTestConfig extends Config {
        @ConfigSetting
        private boolean booleanValue = false;

        // Names are converted to kebab case by default.
        // This overrides the default.
        @ConfigSetting(name = "dotted.name")
        private String dottedName = "value";

        @ConfigSetting(exclude = { Source.SYSTEM_PROPERTY })
        private Integer excludedSetting = 42;

        @ConfigSetting(exclude = { Source.ENVIRONMENT_VARIABLE })
        private Level level = Level.INFO;

        @ConfigSetting(name = "level", include = { Source.SYSTEM_PROPERTY })
        private Map<String,Level> levels = new HashMap<>();

        public static BaseTestConfig get() {
            return get(BaseTestConfig.class);
        }

        public static BaseTestConfig get(InputStream properties) {
            return get(BaseTestConfig.class, properties);
        }

        public boolean isTrue() {
            return booleanValue;
        }

        public String getDottedName() {
            return dottedName;
        }

        public Integer getExcludedSetting() {
            return excludedSetting;
        }

        public Level getLevel() {
            return level;
        }

        public Level getLevel(String fqcn) {
            return levels.get(fqcn);
        }

        public Map<String,Level> getLevels() {
            return levels;
        }
    }

    @ConfigExtends(BaseTestConfig.class)
    @ConfigNamespace("test.ext")
    public static class ExtendedTestConfig extends Config {
        @ConfigSetting
        private String noise = "";

        @ConfigSetting(operation = Op.ADD)
        private Set<String> addingSet = new HashSet<>();

        @ConfigSetting(operation = Op.REPLACE)
        private Set<String> replacingSet = new HashSet<>();

        @ConfigSetting(operation = Op.ADD)
        private List<String> addingList = new ArrayList<>();

        // Op.REPLACE is the default operation
        @ConfigSetting
        private List<String> replacingList = new ArrayList<>();

        public ExtendedTestConfig() {
            addingSet.add("first");
            replacingSet.add("first");
            addingList.add("first");
            replacingList.add("first");
        }

        public static ExtendedTestConfig get() {
            return get(ExtendedTestConfig.class);
        }

        public static ExtendedTestConfig get(InputStream properties) {
            return get(ExtendedTestConfig.class, properties);
        }

        public String getNoise() {
            return noise;
        }

        public Set<String> getAddingSet() {
            return addingSet;
        }

        public Set<String> getReplacingSet() {
            return replacingSet;
        }

        public List<String> getAddingList() {
            return addingList;
        }

        public List<String> getReplacingList() {
            return replacingList;
        }
    }

    @TempDir
    Path temp;

    @AfterEach
    void tearDown() {
        System.clearProperty("test.boolean-value");
        System.clearProperty("test.dotted.name");
        System.clearProperty("test.excluded-setting");
        System.clearProperty("test.level");
        System.clearProperty("test.level.com.example.Test");
        System.clearProperty("test.ext.noise");
        System.clearProperty("test.ext.adding-set");
        System.clearProperty("test.ext.replacing-set");
        System.clearProperty("test.ext.adding-list");
        System.clearProperty("test.ext.replacing-list");

        // TODO: This method should not exist:
        Config.resetAll();
    }

    @Test
    void test_boolean_systemProperty() {
        System.setProperty("test.boolean-value", "true");
        BaseTestConfig cfg = BaseTestConfig.get();
        assertTrue(cfg.isTrue());
    }

    @Test
    void test_reset() {
        System.setProperty("test.boolean-value", "true");
        BaseTestConfig original = BaseTestConfig.get();
        assertTrue(original.isTrue());

        System.clearProperty("test.boolean-value");
        Config.resetAll();

        BaseTestConfig cfg = BaseTestConfig.get();
        assertFalse(cfg.isTrue());
    }

    @Test
    void test_string_dottedName() {
        System.setProperty("test.dotted.name", "coffee");
        BaseTestConfig cfg = BaseTestConfig.get();
        assertEquals("coffee", cfg.getDottedName());
    }

    @Test
    void test_exclude() {
        System.setProperty("test.excluded-setting", "86");
        BaseTestConfig cfg = BaseTestConfig.get();
        assertEquals(42, cfg.getExcludedSetting());
    }

    @Test
    void test_enum() {
        System.setProperty("test.level", "debug");
        BaseTestConfig cfg = BaseTestConfig.get();
        assertEquals(Level.DEBUG, cfg.getLevel());
    }

    @Test
    void test_map() {
        System.setProperty("test.level.com.example.Test", "debug");
        BaseTestConfig cfg = BaseTestConfig.get();
        assertEquals(Level.DEBUG, cfg.getLevel("com.example.Test"));
    }

    @Test
    void test_getLevels() {
        System.setProperty("test.level.com.example.Test", "debug");
        BaseTestConfig cfg = BaseTestConfig.get();
        Map<String,Level> map = cfg.getLevels();
        assertEquals(Level.DEBUG, map.get("com.example.Test"));
    }

    @Test
    void test_extendedNamespace() {
        System.setProperty("test.ext.noise", "Woof!");
        ExtendedTestConfig extCfg = ExtendedTestConfig.get();
        assertEquals("Woof!", extCfg.getNoise());
    }

    @Test
    void test_specifyPropertiesFile() throws IOException {
        Path appProperties = temp.resolve("app.properties");
        Files.writeString(appProperties, """
            ext.noise = Meow!
            """);

        try (InputStream is = Files.newInputStream(appProperties)) {
            BaseTestConfig.get(is);
        }

        ExtendedTestConfig extCfg = ExtendedTestConfig.get();

        assertNotNull(extCfg);
        assertEquals("Meow!", extCfg.getNoise());
    }

    @Test
    void test_adding_set() {
        System.setProperty("test.ext.adding-set", "second, third");

        ExtendedTestConfig cfg = ExtendedTestConfig.get();

        assertEquals(3, cfg.getAddingSet().size());
        assertTrue(cfg.getAddingSet().contains("first"));
        assertTrue(cfg.getAddingSet().contains("second"));
        assertTrue(cfg.getAddingSet().contains("third"));
    }

    @Test
    void test_replacing_set() {
        System.setProperty("test.ext.replacing-set", "second, third");

        ExtendedTestConfig cfg = ExtendedTestConfig.get();

        assertEquals(2, cfg.getReplacingSet().size());
        assertTrue(cfg.getReplacingSet().contains("second"));
        assertTrue(cfg.getReplacingSet().contains("third"));
    }

    @Test
    void test_adding_list() {
        System.setProperty("test.ext.adding-list", "second, third");

        ExtendedTestConfig cfg = ExtendedTestConfig.get();

        assertEquals(3, cfg.getAddingList().size());
        assertTrue(cfg.getAddingList().contains("first"));
        assertTrue(cfg.getAddingList().contains("second"));
        assertTrue(cfg.getAddingList().contains("third"));
    }

    @Test
    void test_replacing_list() {
        System.setProperty("test.ext.replacing-list", "second, third");

        ExtendedTestConfig cfg = ExtendedTestConfig.get();

        assertEquals(2, cfg.getReplacingList().size());
        assertTrue(cfg.getReplacingList().contains("second"));
        assertTrue(cfg.getReplacingList().contains("third"));
    }

}
