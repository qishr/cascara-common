package io.github.qishr.cascara.common.diagnostic;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class GlobalReporter extends AbstractReporter<GlobalReporter> {
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static GlobalReporter globalInstance;

    private GlobalReporter(Class<?> clazz) {
        this.clazz = clazz;
    }

    private GlobalReporter() {
        // Nothing to see here
    }

    @Override
    protected GlobalReporter self() { return this; }

    public static GlobalReporter globalInstance() {
        if (globalInstance == null) {
            globalInstance = new GlobalReporter();
        }
        return globalInstance;
    }

    public static GlobalReporter forClass(Class<?> clazz) {
        globalInstance();
        GlobalReporter reporter = new GlobalReporter(clazz);
        return reporter;
    }

    @Override
    public GlobalReporter setLevel(Level level) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setLevel in GlobalReporter may only be called on the global instance.");
        }
        this.level = level;
        return this;
    }

    @Override
    public GlobalReporter setDiagnosticCollector(Consumer<Diagnostic> collector) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDiagnosticWriter in GlobalReporter may only be called on the global instance.");
        }
        this.diagnosticCollector = collector;
        return this;
    }

    @Override
    public GlobalReporter setProblemCollector(Consumer<Diagnostic> collector) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setCollector in GlobalReporter may only be called on the global instance.");
        }
        this.problemCollector = collector;
        return this;
    }

    public GlobalReporter setDisableSystemOutput(boolean b) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDisableSystemOutput in GlobalReporter may only be called on the global instance.");
        }
        globalInstance.disableSystemOutput = b;
        return this;
    }

    public GlobalReporter setDisableFlush(boolean b) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDisableFlush in GlobalReporter may only be called on the global instance.");
        }
        globalInstance.disableFlush = b;
        return this;
    }

    //
    //
    //

    @Override
    protected Consumer<Diagnostic> getDiagnosticCollector() { return globalInstance.diagnosticCollector; }

    @Override
    protected Consumer<Diagnostic> getProblemCollector() { return globalInstance.problemCollector; }

    @Override
    protected Consumer<String> getStringWriter() { return globalInstance.stringWriter; }

    @Override
    protected boolean disableSystemOutput() { return globalInstance.disableSystemOutput; }

    @Override
    protected boolean disableFlush() { return globalInstance.disableFlush; }

    @Override
    protected void writeString(Diagnostic diagnostic) {
        if (diagnostic.getUri() == null) {
            writeString (diagnostic.getLevel(),
                String.format(
                    "[%5s] [%s] [%s] %s\n",
                    diagnostic.getLevel(),
                    diagnostic.getTime().format(TIME_FORMAT),
                    diagnostic.getClazz(),
                    diagnostic.getMessage()
                )
            );
        } else {
            if (diagnostic.getLine() > 0) {
                writeString (diagnostic.getLevel(),
                    String.format(
                        "[%5s] [%s] [%s] %s at %s:%d\n",
                        diagnostic.getLevel(),
                        diagnostic.getTime().format(TIME_FORMAT),
                        diagnostic.getClazz(),
                        diagnostic.getMessage(),
                        diagnostic.getUri(),
                        diagnostic.getLine()
                    )
                );
            } else {
                if (diagnostic.getLine() > 0) {
                    writeString (diagnostic.getLevel(),
                        String.format(
                            "[%5s] [%s] [%s] %s in file %s\n",
                            diagnostic.getLevel(),
                            diagnostic.getTime().format(TIME_FORMAT),
                            diagnostic.getClazz(),
                            diagnostic.getMessage(),
                            diagnostic.getUri()
                        )
                    );
                }
            }
        }
    }
}
