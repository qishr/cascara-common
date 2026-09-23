package io.github.qishr.cascara.common.diagnostic;

public class NoOpReportWriter extends ReportWriter {

    NoOpReportWriter() {
        super(null,null);
    }

    public void write(int indent, String str) {}

    //
    //
    //

    @Override
    public void write(char[] cbuf, int off, int len) {}

    @Override
    public void flush() {}

    @Override public void close() {}

    void outputLine(String msgLine, int msgLineNumber) {}

    void logLine(String msgLine, int msgLineNumber) {}

}