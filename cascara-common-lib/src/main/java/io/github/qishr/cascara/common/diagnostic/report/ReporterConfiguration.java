package io.github.qishr.cascara.common.diagnostic.report;

import java.util.HashMap;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.ConfigurationProperty;
import io.github.qishr.cascara.common.annotation.PropertiesNamespace;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.util.ConfigurationProperties;

@PropertiesNamespace("cascara.reporter")
public class ReporterConfiguration extends ConfigurationProperties {
    @ConfigurationProperty(name = "cascara.reporter.level.*")
    private Map<String,Level> levels = new HashMap<>();

    @ConfigurationProperty
    private boolean autoFlush = false;

    public ReporterConfiguration() {}

    public Level getLevel(String fqcn) {
        return getMapValue("levels", Level.class, fqcn);
    }

    public Map<String,Level> getLevels() {
        return getMap("levels", Level.class);
    }

    public boolean isAutoFlush() {
        return getBoolean("autoFlush");
    }
}
