package io.github.qishr.cascara.common.diagnostic;

import java.util.function.Consumer;

public class SimpleReporter extends AbstractReporter<SimpleReporter> {
    public SimpleReporter(Consumer<String> writer) {
        super(writer);
    }

    public SimpleReporter() {
        // Nothing to see here
    }

    @Override
    protected SimpleReporter self() { return this; }

    @Override
    protected void writeString(Diagnostic diagnostic) {
        if (diagnostic.getUri() == null) {
            writeString (diagnostic.getLevel(),
                String.format(
                    "[%5s] %s\n",
                    diagnostic.getLevel(),
                    diagnostic.getMessage()
                )
            );
        } else {
            if (diagnostic.getLine() > 0) {
                writeString (diagnostic.getLevel(),
                    String.format(
                        "[%5s] %s at %s:%d\n",
                        diagnostic.getLevel(),
                        diagnostic.getMessage(),
                        diagnostic.getUri(),
                        diagnostic.getLine()
                    )
                );
            } else {
                if (diagnostic.getLine() > 0) {
                    writeString (diagnostic.getLevel(),
                        String.format(
                            "[%5s] %s in file %s\n",
                            diagnostic.getLevel(),
                            diagnostic.getMessage(),
                            diagnostic.getUri()
                        )
                    );
                }
            }
        }
    }
}
