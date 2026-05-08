package io.github.qishr.cascara.common.diagnostic;

import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class SimpleReporter implements Reporter {
    private Class<?> clazz;
    private Level level = Level.INFO;
    private ReportStringWriter stringWriter;
    private ReportDiagnosticWriter diagnosticWriter;
    private ReportCollector collector;
    private boolean disableSystemOutput = false;
    private boolean disableFlush = true;

    public SimpleReporter(ReportStringWriter writer) {
        this.stringWriter = writer;
    }

    public SimpleReporter() {
        // Nothing to see here
    }

    @Override
    public SimpleReporter forClass(Class<?> clazz) {
        SimpleReporter reporter = new SimpleReporter();
        reporter.clazz = clazz;
        reporter.level = level;
        reporter.stringWriter = stringWriter;
        reporter.diagnosticWriter = diagnosticWriter;
        reporter.collector = collector;
        reporter.disableSystemOutput = disableSystemOutput;
        reporter.disableFlush = disableFlush;
        return reporter;
    }

    @Override
    public SimpleReporter setLevel(Level level) {
        this.level = level;
        return this;
    }

    // @Override
    // public SimpleReporter setWriter(Writer writer) {
    //     this.writer = writer;
    //     return this;
    // }

    @Override
    public SimpleReporter setStringWriter(ReportStringWriter writer) {
        this.stringWriter = writer;
        return this;
    }

    @Override
    public SimpleReporter setDiagnosticWriter(ReportDiagnosticWriter writer) {
        this.diagnosticWriter = writer;
        return this;
    }

    @Override
    public SimpleReporter setCollector(ReportCollector collector) {
        this.collector = collector;
        return this;
    }

    public SimpleReporter setDisableSystemOutput(boolean b) {
        this.disableSystemOutput = b;
        return this;
    }

    public SimpleReporter setDisableFlush(boolean b) {
        this.disableFlush = b;
        return this;
    }

    /// Reports a trace message through the reporter.
    /// @param msg The message to report.
    @Override
    public void trace(Object... msg) {
        MsgAndEx mae = processParams(msg);
        report(Level.TRACE, 0, 0, null, mae.msg(), mae.ex());
    }

    /// Reports a debug message through the reporter.
    /// @param msg The message to report.
    @Override
    public void debug(Object... msg) {
        MsgAndEx mae = processParams(msg);
        report(Level.DEBUG, 0, 0, null, mae.msg(), mae.ex());
    }

    /// Reports an informational message through the reporter.
    /// @param msg The message to report.
    @Override
    public void info(Object... msg) {
        MsgAndEx mae = processParams(msg);
        report(Level.INFO, 0, 0, null, mae.msg(), mae.ex());
    }

    /// Reports a warning message including location information.
    /// @param msg The warning message to report.
    @Override
    public void warn(Object... msg) {
        MsgAndEx mae = processParams(msg);
        report(Level.WARNING, 0, 0, null, mae.msg(), mae.ex());
    }

    /// Reports an error message including location information.
    /// @param msg The error message to report.
    @Override
    public void error(Object... msg) {
        MsgAndEx mae = processParams(msg);
        report(Level.ERROR, 0, 0, null, mae.msg(), mae.ex());
    }

    //
    // With Location
    //

    @Override
    public void infoAt(int line, int column, URI uri, Object... msg) {
        MsgAndEx mae = processParams(msg);
        report(Level.INFO, line, column, uri, mae.msg(), mae.ex());
    }

    @Override
    public void warnAt(int line, int column, URI uri, Object... msg) {
        MsgAndEx mae = processParams(msg);
        report(Level.WARNING, line, column, uri, mae.msg(), mae.ex());
    }

    @Override
    public void errorAt(int line, int column, URI uri, Object... msg) {
        MsgAndEx mae = processParams(msg);
        report(Level.ERROR, line, column, uri, mae.msg(), mae.ex());
    }

    //
    //
    //

    private void report(Level level, int line, int column, URI uri, String message, Exception exception) {
        if (collector == null || (level != Level.WARNING && level != Level.ERROR)) {
            if (this.level.compareTo(level) >= 0) {
                writeString(level, line, column, uri, message, exception);
            }
            if (this.diagnosticWriter != null) {
                writeDiagnostic(level, line, column, uri, message, exception);
            }
        } else {
            collect(level, line, column, uri, message, exception);
        }
    }

    private void collect(Level level, int line, int column, URI uri, String message, Exception exception) {
        Diagnostic diagnostic = new Diagnostic(clazz, level, message, line, column, uri, exception);
        collector.collect(diagnostic);
    }

    private void writeDiagnostic(Level level, int line, int column, URI uri, String message, Exception exception) {
        Diagnostic diagnostic = new Diagnostic(clazz, level, message, line, column, uri, exception);
        diagnosticWriter.write(diagnostic);
    }

    private void writeString(Level level, int line, int column, URI uri, String message, Exception exception) {
        // Write to console / writer
        if (uri == null) {
            writeString (level, "[" + level + "] " + message + "\n");
        } else {
            if (line > 0) {
                writeString (level, "[" + level + "] " + message + " at " + uri + ":" + line + ":" + column+ "\n");
            } else {
                writeString (level, "[" + level + "] " + message + " in file: " + uri + "\n");
            }
        }
    }

    private void writeString(Level level, String text) {
        PrintStream console = level == Level.ERROR ? System.err : System.out;
        if (!disableSystemOutput) {
            console.print(text);
            if (!disableFlush) {
                console.flush();
            }
        }
        if (stringWriter != null) {
            stringWriter.write(text);
        }
    }

    //
    //
    //

    private MsgAndEx processParams(Object... s) {
        if (s == null || s.length == 0) {
            return new MsgAndEx("Null or empty message", null);
        } else if (s[0] instanceof String m) {
            if (s.length == 1) {
                return new MsgAndEx(m, null);
            } else {
                Object last = s[s.length - 1];
                if (last instanceof Exception e) {
                    return new MsgAndEx(String.format(m, Arrays.<Object>copyOfRange(s, 1, s.length - 1)), e);
                } else {
                    return new MsgAndEx(String.format(m, Arrays.<Object>copyOfRange(s, 1, s.length)), null);
                }
            }
        } else {
            return new MsgAndEx("First parameter must be of type String", null);
        }
    }

    private static record MsgAndEx(String msg, Exception ex) {}
}
