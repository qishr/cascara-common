package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;

public class Diagnostic {
    private final Class<?> clazz;
    private final Level level;
    private final String message;
    private final int line;
    private final int column;
    private final URI uri;
    private final Exception exception;

    public Diagnostic(Class<?> clazz, Level level, String message, int line, int column, URI uri, Exception exception) {
        this.clazz = clazz;
        this.level = level;
        this.message = message;
        this.line = line;
        this.column = column;
        this.uri = uri;
        this.exception = exception;
    }

    public Class<?> getClazz() { return clazz; }
    public Level getLevel() { return level; }
    public String getMessage() { return message; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public URI getUri() { return uri; }
    public Exception getException() { return exception; }

    public enum Level {
        ERROR(1),
        WARNING(2),
        INFO(3),
        DEBUG(4),
        TRACE(5);

        private int level;

        Level(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }
}
