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


package io.github.qishr.cascara.common.diagnostic.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.DiagnosticBuilder;
import io.github.qishr.cascara.common.diagnostic.format.ColoredLogFormatter;
import io.github.qishr.cascara.common.diagnostic.format.PlainLogFormatter;
import io.github.qishr.cascara.common.diagnostic.log.ConsoleLogger;
import io.github.qishr.cascara.common.diagnostic.log.LineLogger;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.property.Properties;
import io.github.qishr.cascara.common.property.Property;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.util.Pair;
import io.github.qishr.cascara.common.util.ReflectionUtils;

public class GlobalReporter extends AbstractReporter<GlobalReporter> {
    private static final String PROP_CASC_REPORT_LEVEL = "casc.report.level.";
    private static final String ENV_CASC_REPORT_LEVEL = "CASC_REPORT_LEVEL_";

    public static final String SERIALIZATION_FORMAT = "application/json";

    // private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private static GlobalReporter globalInstance = new GlobalReporter().init();
    private final Map<String,GlobalReporter> classInstances = new HashMap<>();

    private final Map<String,Level> envLevels = new HashMap<>();

    private boolean allowApiOverride;

    private IpcClient ipcClient;
    private boolean ipcUnavailable = false;
    private List<Diagnostic> queue = new ArrayList<>();
    // private long processId;


    private GlobalReporter() {
    }

    private GlobalReporter init() {
        globalInstance = this;

        consoleLogger = new ConsoleLogger();
        consoleLogger.setFormatter(new ColoredLogFormatter());

        lineLogger = new LineLogger();
        lineLogger.setFormatter(new PlainLogFormatter());


        // TODO:
        // CASC_REPORT_CONFIG=/path/


        // This will only ever be used by apps like Studio.
        // Tests will always use a new JvmProcess and set reporting options with -D
        // CASC_REPORT_ALLOW_API_OVERRIDE=true
        String allowApiOverride = System.getenv("CASC_REPORT_ALLOW_API_OVERRIDE");
        this.allowApiOverride = (allowApiOverride != null && allowApiOverride.equalsIgnoreCase("true"));

        // System properties...
        // -Dcasc.report.config=
        // -Dcasc.report.level.fqcn=
        Properties systemProperties = Properties.fromSystemProperties();
        setLevels(systemProperties);

        // System environment...
        // CASC_REPORT_LEVEL_*
        // CASC_REPORT_LEVEL_COM_FOO=DEBUG
        Map<String,String> env = System.getenv();
        for (Entry<String, String> entry : env.entrySet()) {
            String name = String.valueOf(entry.getKey());
            if (name.startsWith(ENV_CASC_REPORT_LEVEL)) {
                String uppercaseClassName = name.substring(ENV_CASC_REPORT_LEVEL.length());
                Level level = Level.valueOf(entry.getValue());
                if (level != null) {
                    // System.out.println("GR-INIT: " + uppercaseClassName + " " + level);
                    envLevels.put(uppercaseClassName, level);
                }
            }
        }
        return this;
    }

    private GlobalReporter(String source) {
        this.source = source;
        this.level = globalInstance.level;
    }

    public static void initSerializer(Serializer<?> serializer) {
        if (globalInstance().serializer == null) {
            globalInstance.serializer = serializer;
        }
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
        GlobalReporter reporter = forSourceInternal(fqcn);
        // System.out.println("GLOBALREPORTER for " + fqcn + " " + reporter.level);
        // Thread.dumpStack();
        return reporter;
    }

    /// {@inheritDoc}
    @Override
    public GlobalReporter setLevel(Level level) {
        // TODO: Only throw here if the API call would actually override an environment setting
        // if (!allowApiOverride) {
        //     throw new UnsupportedOperationException("This reporter does not allow setting its level via the API");
        // }
        return setLevelInternal(level);
    }

    /// Sets the reporting level for the specified class.
    /// @param fqcn the fully qualified class name of the class to set the level for.
    /// @param level the level to set for the class.
    public GlobalReporter setLevel(String fqcn, Level level) {
        // TODO: Only throw here if the API call would actually override an environment setting
        if (!allowApiOverride) {
        // if (!allowApiOverride && !fqcn.equals(source)) {
            // throw new UnsupportedOperationException("This reporter does not allow setting its level via the API");
        }
        return setLevelInternal(fqcn, level);
    }

    public GlobalReporter setLevelInternal(String fqcn, Level level) {
        // assertGlobalInstance();
        GlobalReporter reporter = GlobalReporter.forSourceInternal(fqcn);
        reporter.setLevelInternal(level);
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
                    setLevelInternal(fqcn, level);
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

    @Override
    protected Consumer<Diagnostic> getDiagnosticConsumer() {
        return this == globalInstance ? diagnosticConsumer : globalInstance.getDiagnosticConsumer();
    }

    @Override
    protected Consumer<Diagnostic> getProblemConsumer() {
        return this == globalInstance ? problemConsumer : globalInstance.getProblemConsumer();
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
        // TODO: They shoul all be like this?
        return stackTraceEnabled || globalInstance.stackTraceEnabled;
    }

    @Experimental
    public void closeIpc() {
        if (globalInstance.ipcClient != null) {
            try {
                globalInstance.ipcClient.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                globalInstance.ipcClient = null;
            }
        }
    }

    /// Central reporting method. All other rporting methods call this.
    @Override
    public void report(Diagnostic diagnostic) {
        if (this == globalInstance) {
            super.reportAnyLevel(diagnostic);
            if (SPL.isBooting()) {
                globalInstance.queue.add(diagnostic);
            } else {
                globalInstance.sendDiagnostic(diagnostic);
            }
        } else {
            if (this.level.includes(diagnostic.getLevel())) {
                globalInstance.report(diagnostic);
            }
        }
    }

    @Override
    protected void logLine(Level level, String msgLine) {
        // if (getLineConsumer() != null) {
        //     String logLine = "[" + level.getLogPrefix() + "] " + msgLine;
        //     getLineConsumer().accept(logLine);
        // }
        Diagnostic diagnostic = DiagnosticBuilder.build("", level, msgLine);
        globalInstance.log(diagnostic);
    }

    //
    //
    //

    private static GlobalReporter forSourceInternal(String fqcn) {
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

    private GlobalReporter setLevelInternal(Level level) {
        if (this != globalInstance) {
            setLevelsForAll(level);
        } else {
            this.level = level;
        }
        return this;
    }

    private void sendDiagnostic(Diagnostic diagnostic) {
        if (ipcUnavailable || serializer == null) {
            return;
        }

        if (ipcClient == null) {
            try {
                ipcClient = IpcClient.tryConnect(serializer);
            } catch (Exception e) {
                // TODO: proper exception handling
                System.out.println("GR-IPC ERROR: " + e.getMessage());
                // There was no IpcServer to connect to, which is okay
                ipcUnavailable = true;
            }
        }

        if (ipcUnavailable || ipcClient == null) {
            // Discard queued diagnostics
            queue.clear();
        } else {
            try {
                // TODO: Send queued diagnostics
                if (!queue.isEmpty()) {
                    // System.out.println("GR-IPC: Sending queued diagnostics");
                    for (Diagnostic d : queue) {
                        if (ipcClient.getServerProcessId() != d.getProcessId()) {
                            ipcClient.send(d);
                        }
                    }
                    queue.clear();
                }

                // TODO: This guard should use the JVM ID (with host name)
                // instead of just PID
                if (ipcClient.getServerProcessId() != diagnostic.getProcessId()) {
                    ipcClient.send(diagnostic);
                }

            } catch (IOException e) {
                e.printStackTrace();
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
