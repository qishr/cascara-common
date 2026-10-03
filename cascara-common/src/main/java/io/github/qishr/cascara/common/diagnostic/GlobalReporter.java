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


package io.github.qishr.cascara.common.diagnostic;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.property.Property;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public class GlobalReporter extends AbstractReporter<GlobalReporter> {
    private static final String PROP_CASC_REPORT_LEVEL = "casc.report.level.";
    private static final String ENV_CASC_REPORT_LEVEL = "CASC_REPORT_LEVEL_";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private static final GlobalReporter globalInstance = new GlobalReporter();
    private final Map<String,GlobalReporter> classInstances = new HashMap<>();

    private final Map<String,Level> envLevels = new HashMap<>();

    private boolean allowApiOverride;
    private Path reportConfigPath;

    private GlobalReporter() {
        // TODO:
        // CASC_REPORT_CONFIG=/path/

        // CASC_REPORT_ALLOW_API_OVERRIDE=true
        String allowApiOverride = System.getenv("CASC_REPORT_ALLOW_API_OVERRIDE");
        this.allowApiOverride = (allowApiOverride != null && allowApiOverride.toLowerCase().equals("true"));

        // System properties...
        // -Dcasc.report.config=
        // -Dcasc.report.level.fqcn=
        setLevels(Properties.fromSystemProperties());

        // System environment...
        // CASC_REPORT_LEVEL_*
        // CASC_REPORT_LEVEL_COM_FOO=DEBUG
        Map<String,String> env = System.getenv();
        for (Entry<String, String> entry : System.getenv().entrySet()) {
            String name = String.valueOf(entry.getKey());
            if (name.startsWith(ENV_CASC_REPORT_LEVEL)) {
                String uppercaseClassName = name.substring(ENV_CASC_REPORT_LEVEL.length());
                Level level = Level.valueOf(entry.getValue());
                if (level != null) {
                    envLevels.put(uppercaseClassName, level);
                }
            }
        }
    }

    private GlobalReporter(String source) {
        this.source = source;
        this.level = globalInstance.level;
    }

    /// {@inheritDoc}
    @Override
    protected GlobalReporter self() { return this; }

    public static GlobalReporter globalInstance() {
        return globalInstance;
    }

    /// Gets a `GlobalReporter` instance for the specifie class.
    /// @param clazz the class to get a `GlobalReporter` instance for.
    public static GlobalReporter forClass(Class<?> clazz) {
        return forSource(clazz.getName());
    }

    /// Gets a `GlobalReporter` instance for the specifie class.
    /// @param fqcn the fully qualified name of the class to get a `GlobalReporter` instance for.
    public static GlobalReporter forSource(String fqcn) {
        GlobalReporter reporter = globalInstance.classInstances.get(fqcn);
        if (reporter == null) {
            reporter = new GlobalReporter(fqcn);
            globalInstance.classInstances.put(fqcn, reporter);
            String envKey = fqcn.replace('.', '_').toUpperCase();
            Level envLevel = globalInstance.envLevels.get(envKey);
            if (envLevel != null) {
                reporter.level = envLevel;
            }
        }
        return reporter;
    }

    /// {@inheritDoc}
    @Override
    public GlobalReporter setLevel(Level level) {
        if (this != globalInstance) {
            setLevelsForAll(level);
        } else {
            this.level = level;
        }
        return this;
    }

    /// Sets the reporting level for the specified class.
    /// @param fqcn the fully qualified class name of the class to set the level for.
    /// @param level the level to set for the class.
    public GlobalReporter setLevel(String fqcn, Level level) {
        assertGlobalInstance();
        GlobalReporter reporter = GlobalReporter.forSource(fqcn);
        reporter.setLevel(level);
        return this;
    }

    // TODO: support full JlsName
    public GlobalReporter setLevels(Properties properties) {
        for (Property<?> property : properties) {
            String propertyName = property.getName();
            if (propertyName.startsWith(PROP_CASC_REPORT_LEVEL)) {
                String fqcn = propertyName.substring(PROP_CASC_REPORT_LEVEL.length());
                Level level = Level.valueOf(property.asString());
                if (level != null) {
                    setLevel(fqcn, level);
                }
            }
        }
        return this;
    }

    @Override
    public GlobalReporter setDiagnosticConsumer(Consumer<Diagnostic> collector) {
        assertGlobalInstance();
        super.setDiagnosticConsumer(collector);
        return this;
    }

    @Override
    public GlobalReporter setProblemConsumer(Consumer<Diagnostic> collector) {
        assertGlobalInstance();
        super.setProblemConsumer(collector);
        return this;
    }

    public GlobalReporter setSystemOutputEnabled(boolean b) {
        assertGlobalInstance();
        super.setSystemOutputEnabled(b);
        return this;
    }

    public GlobalReporter setFlushEnabled(boolean b) {
        assertGlobalInstance();
        super.setFlushEnabled(b);
        return this;
    }

    //
    //
    //

    @Override
    protected Consumer<Diagnostic> getDiagnosticConsumer() {
        return this == globalInstance ? diagnosticConsumer : globalInstance.getDiagnosticConsumer();
    }

    @Override
    protected Consumer<Diagnostic> getProblemConsumer() {
        return this == globalInstance ? problemConsumer : globalInstance.getProblemConsumer();
    }

    @Override
    protected Consumer<String> getLineConsumer() {
        return this == globalInstance ? lineConsumer : globalInstance.getLineConsumer();
    }

    @Override
    protected boolean isSystemOutputEnabled() {
        return this == globalInstance ? systemOutputEnabled : globalInstance.isSystemOutputEnabled();
    }

    @Override
    protected boolean isFlushEnabled() {
        return this == globalInstance ? flushEnabled : globalInstance.isFlushEnabled();
    }

    @Override
    protected boolean isStackTraceEnabled() {
        return this == globalInstance ? stackTraceEnabled : globalInstance.isStackTraceEnabled();
    }

    @Override
    protected String formatMessage(Diagnostic diagnostic, String message, int messageLine, boolean useColoring) {
        if (diagnostic.getUri() == null) {
            if (diagnostic.getLine() > 0) {
                return String.format(
                    "[%s] [%s] %s at line %d\n",
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getFormattedMessage(),
                    diagnostic.getLine()
                );
            } else {
                return String.format(
                    "[%s] [%s] %s\n",
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getFormattedMessage()
                );
            }
        } else {
            if (diagnostic.getLine() > 0) {
                return String.format(
                    "[%s] [%s] %s at %s:%d\n",
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getFormattedMessage(),
                    diagnostic.getUri(),
                    diagnostic.getLine()
                );
            } else {
                return String.format(
                    "[%s] [%s] %s in file %s\n",
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getFormattedMessage(),
                    diagnostic.getUri()
                );
            }
        }
    }

    private void setLevelsForAll(Level level) {
        this.level = level;
        for (GlobalReporter reporter : classInstances.values()) {
            reporter.level = level;
        }
    }

    private void assertGlobalInstance() {
        if (this != globalInstance) {
            Pair<Class<?>,String> caller = ReflectionUtils.getCaller();
            String methodName = caller.getR();
            String msg = String.format(
                "The method %s in GlobalReporter may only be called on the global instance.",
                methodName
            );
            throw new UnsupportedOperationException(msg);
        }
    }
}
