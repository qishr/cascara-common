package io.github.qishr.cascara.common.diagnostic;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class GlobalReporter extends AbstractReporter<GlobalReporter> {
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final GlobalReporter globalInstance = new GlobalReporter();

    private static final Map<String,GlobalReporter> classInstances = new HashMap<>();

    private GlobalReporter(String source) {
        this.source = source;
        this.level = globalInstance.level;
    }

    private GlobalReporter() {
        // Nothing to see here
    }

    /// {@inheritDoc}
    @Override
    protected GlobalReporter self() { return this; }

    public static GlobalReporter globalInstance() {
        return globalInstance;
    }

    public static GlobalReporter forClass(Class<?> clazz) {
        return forSource(clazz.getSimpleName());
    }

    public static GlobalReporter forSource(String source) {
        GlobalReporter reporter = classInstances.get(source);
        if (reporter == null) {
            reporter = new GlobalReporter(source);
            classInstances.put(source, reporter);
        }
        return reporter;
    }

    @Override
    public GlobalReporter setLevel(Level level) {
        this.level = level;
        return this;
    }

    @Override
    public GlobalReporter setDiagnosticCollector(Consumer<Diagnostic> collector) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDiagnosticWriter in GlobalReporter may only be called on the global instance.");
        }
        super.setDiagnosticCollector(collector);
        return this;
    }

    @Override
    public GlobalReporter setProblemCollector(Consumer<Diagnostic> collector) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setCollector in GlobalReporter may only be called on the global instance.");
        }
        super.setProblemCollector(collector);
        return this;
    }

    public GlobalReporter setDisableSystemOutput(boolean b) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDisableSystemOutput in GlobalReporter may only be called on the global instance.");
        }
        super.setDisableSystemOutput(b);
        return this;
    }

    public GlobalReporter setDisableFlush(boolean b) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDisableFlush in GlobalReporter may only be called on the global instance.");
        }
        super.setDisableFlush(b);
        return this;
    }

    //
    //
    //

    @Override
    protected Consumer<Diagnostic> getDiagnosticCollector() {
        return this == globalInstance ? diagnosticCollector : globalInstance.getDiagnosticCollector();
    }

    @Override
    protected Consumer<Diagnostic> getProblemCollector() {
        return this == globalInstance ? problemCollector : globalInstance.getProblemCollector();
    }

    @Override
    protected Consumer<String> getStringWriter() {
        return this == globalInstance ? stringWriter : globalInstance.getStringWriter();
    }

    @Override
    protected boolean disableSystemOutput() {
        return this == globalInstance ? disableSystemOutput : globalInstance.disableSystemOutput();
    }

    @Override
    protected boolean disableFlush() {
        return this == globalInstance ? disableFlush : globalInstance.disableFlush();
    }

    @Override
    protected boolean printStackTrace() {
        return this == globalInstance ? printStackTrace : globalInstance.printStackTrace();
    }

    @Override
    protected void writeString(Diagnostic diagnostic) {
        if (diagnostic.getUri() == null) {
            writeString (
                diagnostic.getCause(),
                diagnostic.getLevel(),
                String.format(
                    "[%5s] [%s] [%s] %s\n",
                    diagnostic.getLevel(),
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getMessage()
                )
            );
        } else {
            if (diagnostic.getLine() > 0) {
                writeString (
                    diagnostic.getCause(),
                    diagnostic.getLevel(),
                    String.format(
                        "[%5s] [%s] [%s] %s at %s:%d\n",
                        diagnostic.getLevel(),
                        diagnostic.getTimestamp().format(TIME_FORMAT),
                        diagnostic.getSource(),
                        diagnostic.getMessage(),
                        diagnostic.getUri(),
                        diagnostic.getLine()
                    )
                );
            } else {
                writeString (
                    diagnostic.getCause(),
                    diagnostic.getLevel(),
                    String.format(
                        "[%5s] [%s] [%s] %s in file %s\n",
                        diagnostic.getLevel(),
                        diagnostic.getTimestamp().format(TIME_FORMAT),
                        diagnostic.getSource(),
                        diagnostic.getMessage(),
                        diagnostic.getUri()
                    )
                );
            }
        }
    }
}
