package io.github.qishr.cascara.common.diagnostic;

import java.net.URI;
import java.time.LocalDateTime;

import io.github.qishr.cascara.common.lang.token.Token;

public class Diagnostic {
    private final LocalDateTime time;
    private final Class<?> clazz;
    private final Level level;
    private final String message;
    private final int startOffset;
    private final int endOffset;
    private final int line;
    private final int column;
    private final URI uri;
    private final Throwable cause;

    public Diagnostic(Class<?> clazz, Level level, String message, int startOffset, int endOffset, int line, int column, URI uri, Throwable cause) {
        this.time = LocalDateTime.now();
        this.clazz = clazz;
        this.level = level;
        this.message = message;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.line = line;
        this.column = column;
        this.uri = uri;
        this.cause = cause;
    }

    public Diagnostic(Class<?> clazz, Level level, String message, Token token, URI uri, Throwable cause) {
        this(clazz,
            level,
            message,
            token.getOffset(),
            token.getOffset() + token.getLexeme().length(),
            token.getStartLine(),
            token.getStartColumn(),
            uri,
            cause);
    }

    public LocalDateTime getTime() { return time; }
    public Class<?> getClazz() { return clazz; }
    public Level getLevel() { return level; }
    public String getMessage() { return message; }
    public int getStartOffset() { return startOffset; }
    public int getEndOffset() { return endOffset; }
    public int getLine() { return line; }
    public int getColumn() { return column; }
    public URI getUri() { return uri; }
    public Throwable getCause() { return cause; }

    public enum Level {
        DEFAULT(0),
        ERROR(1),
        WARN(2),
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
