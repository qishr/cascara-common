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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.format.FormatConfig;
import io.github.qishr.cascara.common.diagnostic.DiagnosticBuilder;
import io.github.qishr.cascara.common.diagnostic.log.ConsoleLogger;
import io.github.qishr.cascara.common.diagnostic.log.LineLogger;
import io.github.qishr.cascara.common.exec.ipc.IpcClient;
import io.github.qishr.cascara.common.exec.ipc.IpcConfig;
import io.github.qishr.cascara.common.exec.ipc.IpcException;
import io.github.qishr.cascara.common.lang.processor.Serializer;
import io.github.qishr.cascara.common.service.SPL;
import io.github.qishr.cascara.common.type.cascara.LocalizableMessageDescriptor;
import io.github.qishr.cascara.common.type.lang.StackTraceElementSerializer;
import io.github.qishr.cascara.common.type.time.InstantDescriptor;
import io.github.qishr.cascara.common.type.time.ZonedDateTimeDescriptor;

public class GlobalReporter extends ClassReporter implements DiagnosticReporter {
    public static final String SERIALIZATION_FORMAT = "application/json";

    private static GlobalReporter globalInstance = new GlobalReporter().init();

    private static ReporterConfig config;

    private final Map<String,ClassReporter> classInstances = new HashMap<>();

    boolean allowApiOverride;

    private IpcClient ipcClient;
    private boolean ipcUnavailable = false;
    private List<Diagnostic> queue = new ArrayList<>();

    /// Consumes diagnostics included in the current Level or more
    /// important, with ERROR being the most important.
    protected Set<Consumer<Diagnostic>> diagnosticConsumers = new HashSet<>();

    /// Consumes ERROR, WARN, and INFO diagnostics.
    protected Consumer<Diagnostic> problemConsumer;

    GlobalReporter() {}

    private GlobalReporter init() {
        globalInstance = this;

        consoleLogger = new ConsoleLogger();
        consoleLogger.setFormatConfig(formatConfig);

        lineLogger = new LineLogger();
        lineLogger.setFormatConfig(formatConfig);

        // TODO: support full JlsName

        config = ReporterConfig.get();

        allowApiOverride = config.allowApiOverride();

        setLevelInternal(config.getLevel());

        // These levels include settings from system properties and properties files
        for (Entry<String,Level> property : config.getLevels().entrySet()) {
            String fqcn = property.getKey();
            Level level = property.getValue();
            if (level != null) {
                setLevelInternal(fqcn, level);
            }
        }

        return this;
    }

    //
    //
    //

    public static GlobalReporter globalInstance() {
        return globalInstance;
    }

    /// Gets a `GlobalReporter` instance for the specifie class.
    /// @param clazz the class to get a `GlobalReporter` instance for.
    public static ClassReporter forClass(Class<?> clazz) {
        return forSource(clazz.getName());
    }

    /// Gets a `GlobalReporter` instance for the specifie class.
    /// @param fqcn the fully qualified name of the class to get a `GlobalReporter` instance for.
    public static ClassReporter forSource(String fqcn) {
        ClassReporter reporter = forSourceInternal(fqcn);
        return reporter;
    }

    public static void configureSerializer() {
        try {
            Serializer<?> serializer = IpcConfig.createSerializer();
            if (serializer != null) {
                // Prevent the serializer from using SPL to find TypeDescriptor and
                // TypeSerializer providers to avoid infinite recursion:
                //     SPL -> GlobalReporter -> Serializer -> SPL
                serializer.setProviderFactoryEnabled(false);

                // Instead of letting the Serializer use SPL, give it the type
                // descriptors and type serializers it needs for Diagnostic serialization.
                serializer.registerTypeDescriptor(new InstantDescriptor());
                serializer.registerTypeDescriptor(new ZonedDateTimeDescriptor());
                serializer.registerTypeDescriptor(new LocalizableMessageDescriptor());
                serializer.registerTypeDescriptor(new StackTraceElementSerializer());

                if (globalInstance().serializer == null) {
                    globalInstance.serializer = serializer;
                }
            }
        } catch (IpcException ignored) {
            // Reporter will still function without a serializer, so ignore this exception.
        }
    }

    @Experimental
    public FormatConfig getFormatConfig() {
        return formatConfig;
    }

    /// {@inheritDoc}
    @Override
    public GlobalReporter setLevel(Level level) {
        if (!globalInstance.allowApiOverride) {
            throw new UnsupportedOperationException("This reporter does not allow setting its level via the API");
        }
        return setLevelInternal(level);
    }

    /// Sets the reporting level for the specified class.
    /// @param cls the class to set the level for.
    /// @param level the level to set for the class.
    public GlobalReporter setLevel(Class<?> cls, Level level) {
        // TODO: getCanonicalName ?
        if (!globalInstance.allowApiOverride) {
            throw new UnsupportedOperationException("This reporter does not allow setting its level via the API");
        }
        return setLevel(cls.getName(), level);
    }

    /// Sets the reporting level for the specified class.
    /// @param fqcn the fully qualified class name of the class to set the level for.
    /// @param level the level to set for the class.
    public GlobalReporter setLevel(String fqcn, Level level) {
        // TODO: Only throw here if the API call would actually override an environment setting
        if (!globalInstance.allowApiOverride) {
        // if (!allowApiOverride && !fqcn.equals(source)) {
            throw new UnsupportedOperationException("This reporter does not allow setting its level via the API");
        }
        return setLevelInternal(fqcn, level);
    }

    public GlobalReporter setLevelInternal(String fqcn, Level level) {
        ClassReporter reporter = GlobalReporter.forSourceInternal(fqcn);
        reporter.setLevelInternal(level);
        return this;
    }

    @Experimental
    protected Set<Consumer<Diagnostic>> getDiagnosticConsumers() {
        return diagnosticConsumers;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public GlobalReporter addDiagnosticConsumer(Consumer<Diagnostic> consumer) {
        diagnosticConsumers.add(consumer);
        return this;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public GlobalReporter removeDiagnosticConsumer(Consumer<Diagnostic> consumer) {
        diagnosticConsumers.remove(consumer);
        return this;
    }

    @Experimental
    protected Consumer<Diagnostic> getProblemConsumer() {
        return problemConsumer;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public GlobalReporter setProblemConsumer(Consumer<Diagnostic> consumer) {
        problemConsumer = consumer;
        return this;
    }

    public GlobalReporter setSystemOutputEnabled(boolean b) {
        super.setSystemOutputEnabled(b);
        return this;
    }

    public GlobalReporter setImmediateFlushEnabled(boolean b) {
        super.setImmediateFlushEnabled(b);
        return this;
    }

    @Override
    protected boolean isSystemOutputEnabled() {
        return this == globalInstance ? systemOutputEnabled : globalInstance.isSystemOutputEnabled();
    }

    @Override
    protected boolean isImmediateFlushEnabled() {
        return this == globalInstance ? flushEnabled : globalInstance.isImmediateFlushEnabled();
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

    /// Central reporting method. All other reporting methods call this.
    @Override
    public void report(Diagnostic diagnostic) {
        super.reportAnyLevel(diagnostic);

        for (Consumer<Diagnostic> consumer : diagnosticConsumers) {
            consumer.accept(diagnostic);
        }

        if (getProblemConsumer() != null && isProblem(level)) {
            getProblemConsumer().accept(diagnostic);
        }

        if (SPL.hasBooted()) {
            globalInstance.sendDiagnostic(diagnostic);
        } else {
            globalInstance.queue.add(diagnostic);
        }
    }

    @Override
    protected void logLine(Level level, String msgLine) {
        Diagnostic diagnostic = DiagnosticBuilder.build(source, level, msgLine);
        globalInstance.log(diagnostic);
    }

    //
    //
    //

    private static ClassReporter forSourceInternal(String fqcn) {
        ClassReporter reporter = globalInstance.classInstances.get(fqcn);
        if (reporter == null) {
            GlobalReporter root = globalInstance;
            reporter = new ClassReporter(fqcn);
            root.classInstances.put(fqcn, reporter);

            Level level = config.getLevel(fqcn);
            if (level != null) {
                reporter.level = level;
            }
        }
        return reporter;
    }

    GlobalReporter setLevelInternal(Level level) {
        if (this == globalInstance) {
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
                // There was no IpcServer to connect to, which is okay
                ipcUnavailable = true;
            }
        }

        if (ipcUnavailable || ipcClient == null) {
            // Discard queued diagnostics
            // TODO:  write them to System.err if allowed by option (see below)
            queue.clear();
        } else {
            try {
                // Send queued diagnostics
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
                // TODO: Possible option to silence this?
                System.err.println("sendDiagnostic failed (" +
                    e.getMessage() + "): " +
                    diagnostic.getFormattedMessage()
                );
            }
        }
    }

    private void setLevelsForAll(Level level) {
        this.level = level;
        for (ClassReporter reporter : classInstances.values()) {
            reporter.level = level;
        }
    }
}
