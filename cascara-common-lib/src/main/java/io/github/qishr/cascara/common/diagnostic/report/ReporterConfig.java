package io.github.qishr.cascara.common.diagnostic.report;

import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.ConfigProperty;
import io.github.qishr.cascara.common.annotation.ConfigNamespace;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.util.AbstractConfig;

@ConfigNamespace("cascara.report")
public class ReporterConfig extends AbstractConfig {
    @ConfigProperty(name = "level")
    private Map<String,Level> levels = new HashMap<>();

    @ConfigProperty
    private Level level = Level.INFO;

    @ConfigProperty
    private boolean allowApiOverride = true;

    public static ReporterConfig get() {
        return get(ReporterConfig.class);
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

    public boolean allowApiOverride() {
        return allowApiOverride;
    }
}
