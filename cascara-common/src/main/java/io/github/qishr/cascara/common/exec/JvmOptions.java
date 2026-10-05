package io.github.qishr.cascara.common.exec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.Beta;

@Beta
public class JvmOptions {
    private String moduleName;
    private Map<String, String> systemProperties = new HashMap<>();
    private Map<String, String> environmentVariables = new HashMap<>();
    private List<String> addModules = new ArrayList<>();
    private List<String> addOpens = new ArrayList<>();
    private List<String> addReads = new ArrayList<>();
    private String customClassPath;
    private String customModulePath;
    private boolean inheritParentModulePath = true;
    private boolean inheritParentClassPath = true;
    private final List<String> args = new ArrayList<>();
    private Duration timeout = Duration.ofSeconds(10);
    private boolean debug;
    private boolean diagnisticForwaringEnabled = true;;

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public JvmOptions setSystemProperty(String key, String value) {
        this.systemProperties.put(key, value);
        return this;
    }

    public Map<String, String> getEnv() {
        return environmentVariables;
    }

    public JvmOptions setEnv(String key, String value) {
        this.environmentVariables.put(key, value);
        return this;
    }

    public List<String> getModules() {
        return addModules;
    }

    public JvmOptions addModule(String moduleName) {
        this.addModules.add(moduleName);
        return this;
    }

    public List<String> getOpens() {
        return addOpens;
    }

    /// Configures JPMS --add-opens syntax: module/package=targetModule
    /// e.g., "java.base/java.lang=cascara.common"
    public JvmOptions addOpens(String target) {
        this.addOpens.add(target);
        return this;
    }

    public List<String> getReads() {
        return addReads;
    }

    /// Configures JPMS --add-reads syntax: sourceModule=targetModule
    public JvmOptions addReads(String target) {
        this.addReads.add(target);
        return this;
    }

    public String getModulePath() {
        return customModulePath;
    }

    public JvmOptions setModulePath(String modulePath) {
        this.customModulePath = modulePath;
        return this;
    }

    public String getClassPath() {
        return customClassPath;
    }

    public JvmOptions setClassPath(String classPath) {
        this.customClassPath = classPath;
        return this;
    }

    public boolean inheritParentModulePath() {
        return inheritParentModulePath;
    }

    public JvmOptions setInheritParentModulePath(boolean inherit) {
        inheritParentModulePath = inherit;
        return this;
    }

    public boolean inheritParentClassPath() {
        return inheritParentClassPath;
    }

    public JvmOptions setInheritParentClassPath(boolean inherit) {
        inheritParentClassPath = inherit;
        return this;
    }

    public List<String> getArgs() {
        return args;
    }

    public JvmOptions setArg(String arg) {
        this.args.add(arg);
        return this;
    }

    public JvmOptions setArgs(String... args) {
        this.args.addAll(List.of(args));
        return this;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public JvmOptions setTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public boolean debug() {
        return debug;
    }

    public JvmOptions setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public String getModuleName() {
        return moduleName;
    }

    public JvmOptions setModuleName(String name) {
        this.moduleName = name;
        return this;
    }

    public boolean diagnisticForwaringEnabled() {
        return diagnisticForwaringEnabled;
    }

    public JvmOptions setDiagnisticForwaringEnabled(boolean enabled) {
        this.diagnisticForwaringEnabled = enabled;
        return this;
    }
}
