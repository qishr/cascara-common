package io.github.qishr.cascara.common.diagnostic.format;

import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.TermUtils;
import io.github.qishr.cascara.common.util.UriScheme;

public class ColoredLogFormatter extends PlainLogFormatter {
    protected static final boolean CAN_USE_ANSI_COLORING = (
        JreUtils.isRunningInTerminal() ||
        JreUtils.isRunningViaEclipse() ||
        JreUtils.isRunningViaGradle()
        // TODO: A JDK21 way of telling if output is being redirected
    );

    protected static final String[] levelColors = new String[7];
    {
        // levelColors[Level.DEFAULT.ordinal()] = ANSI_WHITE;
        levelColors[Level.ERROR.ordinal()] = TermUtils.ANSI_RED;
        levelColors[Level.WARN.ordinal()] = TermUtils.ANSI_YELLOW;
        levelColors[Level.INFO.ordinal()] = TermUtils.ANSI_BLUE;
        // levelColors[Level.DEBUG.ordinal()] = TermUtils.ANSI_WHITE;
        // levelColors[Level.TRACE.ordinal()] = TermUtils.ANSI_WHITE;
    }

    protected boolean ansiColoringEnabled = true;

    public ColoredLogFormatter() {
        init();
    }

    private void init() {
        processId = ProcessHandle.current().pid();
    }

    @Override
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
            String ansiCode = levelColors[level.ordinal()];
            if (ansiCode != null) {
                sb.append(ansiCode);
                sb.append(level.getLogPrefix());
                sb.append(TermUtils.ANSI_RESET);
            } else {
                sb.append(level.getLogPrefix());
            }
            sb.append("] ");
        } else {
            sb.append("        ");
        }

        if (msgLineNumber == 0) {

            sb.append("[");
            sb.append(timeStamp);
            sb.append("] ");

            sb.append("[");
            if (diagnostic.getProcessId() != processId) {
                sb.append(TermUtils.ANSI_CYAN);
                sb.append(diagnostic.getProcessId());
                sb.append(TermUtils.ANSI_RESET);
            } else {
                sb.append(diagnostic.getProcessId());
            }
            // sb.append(diagnostic.getProcessId());
            sb.append("] ");

            sb.append("[");
            sb.append(diagnostic.getSource());
            sb.append("] ");

            if (showProblemCodes && diagnostic.getLevel().isProblem()) {
                String msgCode = diagnostic.getMessage().getCode();
                sb.append("[");
                sb.append(TermUtils.ANSI_WHITE);
                sb.append(msgCode);
                sb.append(TermUtils.ANSI_RESET);
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
        sb.append(TermUtils.ANSI_RESET);
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
                String ansiCode = levelColors[level.ordinal()];
                if (ansiCode != null) {
                    sb.append(ansiCode);
                    sb.append(level.getLogPrefix());
                    sb.append(TermUtils.ANSI_RESET);
                } else {
                    sb.append(level.getLogPrefix());
                }
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
