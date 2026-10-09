package io.github.qishr.cascara.common.diagnostic.report;

import java.io.IOException;
import java.io.Writer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;

public class ReportWriter extends Writer {
    private final AbstractReporter<?> reporter;
    private final Diagnostic.Level level;
    private final StringBuilder buffer = new StringBuilder();

    ReportWriter(AbstractReporter<?> reporter, Diagnostic.Level level) {
        this.reporter = reporter;
        this.level = level;
    }


    public void write(int indent, String str) {
        try {
            if (indent == 0) {
                write(str.toString(), 0, str.length());
                return;
            }
            StringBuilder sb = new StringBuilder();
            String[] lines = str.split("\n");
            for (int i = 0; i < lines.length; i++) {
                sb.append(" ".repeat(indent));
                sb.append(lines[i]);
                sb.append("\n");
            }
            write(sb.toString(), 0, sb.length());
        } catch (IOException e) {}
    }

    //
    //
    //

    @Override
    public void write(char[] cbuf, int off, int len) {
        // buffer.append(" ".repeat(indent));
        buffer.append(cbuf, off, len);
        flushLines(false);
    }

    @Override
    public void flush() {
        flushLines(true);
    }

    @Override public void close() { flush(); }

    //
    //
    //

    private void flushLines(boolean force) {
        int newlineIdx;
        while ((newlineIdx = buffer.indexOf("\n")) != -1) {
            String line = buffer.substring(0, newlineIdx).replace("\r", "");
            // reporter.displayLine(level, line, msgLineNumber);
            reporter.logLine(level, line);
            buffer.delete(0, newlineIdx + 1);
        }
        if (force && buffer.length() > 0) {
            // reporter.displayLine(level, buffer.toString(), msgLineNumber);
            reporter.logLine(level, buffer.toString());
            buffer.setLength(0);
        }
    }

    // void displayLine(String msgLine, int msgLineNumber) {
    //     flush();
    //     reporter.displayLine(level, msgLine, msgLineNumber);
    // }

    void logLine(String msgLine) {
        flush();
        reporter.logLine(level, msgLine);
    }

}