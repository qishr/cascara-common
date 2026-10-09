package io.github.qishr.cascara.common.diagnostic.format;

import java.net.URI;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.util.JreUtils;
import io.github.qishr.cascara.common.util.TermUtils;
import io.github.qishr.cascara.common.util.UriScheme;

public class LocalLogFormatter implements  LogFormatter {
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

    protected static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_INSTANT;

    protected boolean prefixEveryLine = true;
    protected boolean ansiColoringEnabled = false;
    protected boolean showProblemCodes = false;

    public LocalLogFormatter() {
        init();
    }

    private void init() {
    }

    public LocalLogFormatter withFields(Set<Field> fields) {
        return this;
    }

    @Experimental
    public LocalLogFormatter setPrefixEveryLine(boolean b) {
        prefixEveryLine = b;
        return this;
    }

    public LocalLogFormatter setAnsiColoringEnabled(boolean b) {
        ansiColoringEnabled = CAN_USE_ANSI_COLORING && b;
        return this;
    }

    public String format(Diagnostic diagnostic) {
        StringBuilder sb = new StringBuilder();
        String[] lines = diagnostic.getFormattedMessage().split("\n");
        for (int i = 0; i < lines.length; i++) {
            formatLine(sb, diagnostic, lines[i], i, ansiColoringEnabled);
        }
        if (diagnostic.getStackTrace() != null && diagnostic.getStackTrace().length > 0) {
            sb.append(formatStackTrace(diagnostic.getLevel(), diagnostic.getStackTrace(), diagnostic.getCause()));
        }
        return sb.toString();
    }

    protected void formatLine(StringBuilder sb, Diagnostic diagnostic, String msgLine, int msgLineNumber, boolean colorize) {
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

        if (prefixEveryLine || msgLineNumber == 0) {
            sb.append("[");
            if (ansiColoringEnabled) {
                String ansiCode = levelColors[level.ordinal()];
                if (ansiCode != null) {
                    sb.append(ansiCode);
                    sb.append(level.getLogPrefix());
                    sb.append(TermUtils.ANSI_RESET);
                } else {
                    sb.append(level.getLogPrefix());
                }
            } else {
                sb.append(level.getLogPrefix());
            }
            sb.append("] ");
        } else {
            sb.append("        ");
        }

        if (showProblemCodes && diagnostic.getLevel().isProblem() && msgLineNumber == 0) {
            String msgCode = diagnostic.getMessage().getCode();
            sb.append("[");
            if (colorize) {
                sb.append(TermUtils.ANSI_WHITE);
                sb.append(msgCode);
                sb.append(TermUtils.ANSI_RESET);
            } else {
                sb.append(msgCode);
            }
            sb.append("] ");
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
        if (colorize) {
            sb.append(TermUtils.ANSI_RESET);
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
                if (ansiColoringEnabled) {
                    String ansiCode = levelColors[level.ordinal()];
                    if (ansiCode != null) {
                        sb.append(ansiCode);
                        sb.append(level.getLogPrefix());
                        sb.append(TermUtils.ANSI_RESET);
                    } else {
                        sb.append(level.getLogPrefix());
                    }
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
