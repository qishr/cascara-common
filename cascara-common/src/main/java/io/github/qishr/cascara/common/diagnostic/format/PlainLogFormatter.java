package io.github.qishr.cascara.common.diagnostic.format;

import java.net.URI;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.util.UriScheme;

public class PlainLogFormatter implements LogFormatter {
    protected static final ZoneId UTC = ZoneId.of("UTC");
    protected static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

    protected boolean prefixEveryLine = true;
    protected boolean showProblemCodes = false;
    protected long processId;

    public PlainLogFormatter() {
        init();
    }

    private void init() {
        processId = ProcessHandle.current().pid();
    }

    public PlainLogFormatter withFields(Set<Field> fields) {
        return this;
    }

    public String format(Diagnostic diagnostic) {
        StringBuilder sb = new StringBuilder();
        String[] lines = diagnostic.getFormattedMessage().split("\n");
        for (int i = 0; i < lines.length; i++) {
            formatLine(sb, diagnostic, lines[i], i);
        }
        if (diagnostic.getStackTrace() != null && diagnostic.getStackTrace().length > 0) {
            sb.append(formatStackTrace(diagnostic.getLevel(), diagnostic.getStackTrace(), diagnostic.getCause()));
        }
        return sb.toString();
    }

    @Experimental
    public PlainLogFormatter setPrefixEveryLine(boolean b) {
        prefixEveryLine = b;
        return this;
    }

    protected void formatLine(StringBuilder sb, Diagnostic diagnostic, String msgLine, int msgLineNumber) {
        int diagnosticLineNumber = diagnostic.getLine();
        boolean showLineNumber = diagnosticLineNumber > 0;
        boolean showUri = false;

        String resource = null;
        URI diagnosticUri = diagnostic.getUri();
        if (diagnosticUri != null) {
            resource = UriScheme.of(diagnosticUri) == UriScheme.FILE
                ? Path.of(diagnostic.getUri()).toString()
                : diagnostic.getUri().toString();
            showUri = true;
        }

        Level level = diagnostic.getLevel();
        String timeStamp = ZonedDateTime.ofInstant(diagnostic.getTimestamp(), UTC).format(TIME_FORMAT);

        if (prefixEveryLine || msgLineNumber == 0) {
            sb.append("[");
            sb.append(level.getLogPrefix());
            sb.append("] ");
        } else {
            sb.append("        ");
        }

        if (msgLineNumber == 0) {

            sb.append("[");
            sb.append(timeStamp);
            sb.append("] ");

            sb.append("[");
            sb.append(diagnostic.getProcessId());
            sb.append("] ");

            sb.append("[");
            sb.append(diagnostic.getSource());
            sb.append("] ");

            if (showProblemCodes && diagnostic.getLevel().isProblem()) {
                String msgCode = diagnostic.getMessage().getCode();
                sb.append("[");
                sb.append(msgCode);
                sb.append("] ");
            }
        }

        sb.append(msgLine);

        if (showUri) {
            if (showLineNumber) {
                sb.append(" at ");
                sb.append(resource);
                sb.append(':');
                sb.append(diagnosticLineNumber);
            } else {
                sb.append(" in file ");
                sb.append(resource);
            }
        } else {
            if (showLineNumber) {
                if (diagnostic.getColumn() > 0) {
                    sb.append(" at ");
                    sb.append(diagnosticLineNumber);
                    sb.append(":");
                    sb.append(diagnostic.getColumn());
                } else {
                    sb.append(" at line ");
                    sb.append(diagnosticLineNumber);
                }
            }
        }

        sb.append('\n');
    }

    @Override
    public String formatStackTrace(Level level, StackTraceElement[] stackTrace, Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement frame : stackTrace) {
            String msgLine = String.format(
                "  at %s.%s(%s:%d)",
                frame.getClassName(),
                frame.getMethodName(),
                frame.getFileName(),
                frame.getLineNumber()
            );
            if (prefixEveryLine) {
                sb.append("[");
                sb.append(level.getLogPrefix());
                sb.append("] ");
            } else {
                sb.append("        ");
            }
            sb.append(msgLine);
            sb.append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
