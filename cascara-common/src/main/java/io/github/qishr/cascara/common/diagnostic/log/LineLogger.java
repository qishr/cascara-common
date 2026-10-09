package io.github.qishr.cascara.common.diagnostic.log;

import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.exception.UnimplementedMethodException;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatter;

public class LineLogger implements  Logger {
    /// Consumes every line of diagnostic output as a String.
    protected Consumer<String> lineConsumer;

    private LogFormatter formatter;

    public LineLogger() {
        init();
    }

    // @SingletonInitializer
    private void init() {

    }

    @Override
    public void log(Diagnostic diagnostic) {
        if (formatter == null || lineConsumer == null) {
            return;
        }
        String[] lines = formatter.format(diagnostic).split("\n");
        for (int i = 0; i < lines.length; i++) {
            lineConsumer.accept(lines[i]);
        }
    }

    @Override
    public void flush() {
        throw new UnimplementedMethodException();
    }

    public LineLogger setFormatter(LogFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    public Consumer<String> getLineConsumer() { return lineConsumer; }

    // @Override
    public LineLogger setLineConsumer(Consumer<String> lineConsumer) {
        this.lineConsumer = lineConsumer;
        return this;
    }
}
