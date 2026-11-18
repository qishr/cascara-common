// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

package io.github.qishr.cascara.common.exec;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.qishr.cascara.common.annotation.Beta;

@Beta
public class JvmConfig {
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
    private Duration timeout = Duration.ofSeconds(20);
    private boolean debug;

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public JvmConfig setSystemProperty(String key, Object value) {
        if (value == null) {
            systemProperties.remove(key);
        } else {
            systemProperties.put(key, value.toString());
        }
        return this;
    }

    public Map<String, String> getEnv() {
        return environmentVariables;
    }

    public JvmConfig setEnv(String key, Object value) {
        if (value == null) {
            environmentVariables.remove(key);
        } else {
            environmentVariables.put(key, value.toString());
        }
        return this;
    }

    public List<String> getModules() {
        return addModules;
    }

    public JvmConfig addModule(String moduleName) {
        this.addModules.add(moduleName);
        return this;
    }

    public List<String> getOpens() {
        return addOpens;
    }

    /// Configures JPMS --add-opens syntax: module/package=targetModule
    /// e.g., "java.base/java.lang=cascara.common"
    public JvmConfig addOpens(String target) {
        addOpens.add(target);
        return this;
    }

    public List<String> getReads() {
        return addReads;
    }

    /// Configures JPMS --add-reads syntax: sourceModule=targetModule
    public JvmConfig addReads(String target) {
        addReads.add(target);
        return this;
    }

    public String getModulePath() {
        return customModulePath;
    }

    public JvmConfig setModulePath(String modulePath) {
        customModulePath = modulePath;
        return this;
    }

    public String getClassPath() {
        return customClassPath;
    }

    public JvmConfig setClassPath(String classPath) {
        customClassPath = classPath;
        return this;
    }

    public boolean inheritParentModulePath() {
        return inheritParentModulePath;
    }

    public JvmConfig setInheritParentModulePath(boolean inherit) {
        inheritParentModulePath = inherit;
        return this;
    }

    public boolean inheritParentClassPath() {
        return inheritParentClassPath;
    }

    public JvmConfig setInheritParentClassPath(boolean inherit) {
        inheritParentClassPath = inherit;
        return this;
    }

    public List<String> getArgs() {
        return args;
    }

    public JvmConfig setArg(String arg) {
        args.add(arg);
        return this;
    }

    public JvmConfig setArgs(String... args) {
        this.args.addAll(List.of(args));
        return this;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public JvmConfig setTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public boolean debug() {
        return debug;
    }

    public JvmConfig setDebug(boolean enabled) {
        debug = enabled;
        return this;
    }

    public String getModuleName() {
        return moduleName;
    }

    public JvmConfig setModuleName(String name) {
        moduleName = name;
        return this;
    }
}
