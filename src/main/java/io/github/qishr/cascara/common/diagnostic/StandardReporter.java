package io.github.qishr.cascara.common.diagnostic;

import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class StandardReporter extends AbstractReporter<StandardReporter> {
    public StandardReporter(Consumer<String> writer) {
        super(writer);
    }

    public StandardReporter() {
        // Nothing to see here
    }

    @Override
    protected StandardReporter self() { return this; }

    @Override
    protected void writeString(Diagnostic diagnostic) {
        String message;
        if (diagnostic.getLevel() == Level.DEBUG || diagnostic.getLevel() == Level.TRACE) {
            message = diagnostic.getMessage();
        } else {
            message = DiagnosticLocalizer.DEFAULT.format(diagnostic.getCode(), diagnostic.getDetails());
        }
        if (diagnostic.getUri() == null) {
            if (diagnostic.getLine() > 0) {
                writeString (
                    diagnostic.getCause(),
                    diagnostic.getLevel(),
                    String.format(
                        "[%5s] %s at line %d\n",
                        diagnostic.getLevel(),
                        message,
                        diagnostic.getLine()
                    )
                );
            } else {
                writeString (
                    diagnostic.getCause(),
                    diagnostic.getLevel(),
                    String.format(
                        "[%5s] %s\n",
                        diagnostic.getLevel(),
                        message
                    )
                );
            }
        } else {
            if (diagnostic.getLine() > 0) {
                writeString (
                    diagnostic.getCause(),
                    diagnostic.getLevel(),
                    String.format(
                        "[%5s] %s at %s:%d\n",
                        diagnostic.getLevel(),
                        message,
                        diagnostic.getUri(),
                        diagnostic.getLine()
                    )
                );
            } else {
                writeString (
                    diagnostic.getCause(),
                    diagnostic.getLevel(),
                    String.format(
                        "[%5s] %s in file %s\n",
                        diagnostic.getLevel(),
                        message,
                        diagnostic.getUri()
                    )
                );
            }
        }
    }
}
