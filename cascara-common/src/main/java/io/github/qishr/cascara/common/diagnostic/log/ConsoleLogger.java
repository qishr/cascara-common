package io.github.qishr.cascara.common.diagnostic.log;

import java.io.PrintStream;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.SingletonInitializer;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.exception.UnimplementedMethodException;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatter;
import io.github.qishr.cascara.common.service.ServiceProvider;
import io.github.qishr.cascara.common.util.TermUtils;

@Experimental
public class ConsoleLogger implements Logger, ServiceProvider {
    // protected static final boolean CAN_USE_ANSI_COLORING = (
    //     JreUtils.isRunningInTerminal() ||
    //     JreUtils.isRunningViaEclipse() ||
    //     JreUtils.isRunningViaGradle()
    //     // TODO: A JDK21 way of telling if output is being redirected
    // );

    // protected static final String[] levelColors = new String[7];
    // {
    //     // levelColors[Level.DEFAULT.ordinal()] = ANSI_WHITE;
    //     levelColors[Level.ERROR.ordinal()] = TermUtils.ANSI_RED;
    //     levelColors[Level.WARN.ordinal()] = TermUtils.ANSI_YELLOW;
    //     levelColors[Level.INFO.ordinal()] = TermUtils.ANSI_BLUE;
    //     // levelColors[Level.DEBUG.ordinal()] = TermUtils.ANSI_WHITE;
    //     // levelColors[Level.TRACE.ordinal()] = TermUtils.ANSI_WHITE;
    // }

    protected boolean flushEnabled = false;
    protected boolean systemErrorEnabled = false;
    protected boolean stackTraceEnabled = false;
    protected boolean prefixEveryLine = true;
    protected boolean ansiColoringEnabled;
    protected boolean showProblemCodes = false;

    private LogFormatter formatter;

    // private ConsoleLogger() {}

    @SingletonInitializer
    private void init() {

    }

    @Override
    public void log(Diagnostic diagnostic) {
        writeString(diagnostic);
    }

    @Override
    public void flush() {
        throw new UnimplementedMethodException();
    }

    //
    //
    //

    public ConsoleLogger setFormatter(LogFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    public ConsoleLogger setFlushEnabled(boolean b) {
        flushEnabled = b;
        return this;
    }

    public ConsoleLogger setStackTraceEnabled(boolean b) {
        stackTraceEnabled = b;
        return this;
    }

    public ConsoleLogger setSystemErrorEnabled(boolean b) {
        systemErrorEnabled = b;
        return this;
    }

    // public ConsoleLogger setAnsiColoringEnabled(boolean b) {
    //     ansiColoringEnabled = CAN_USE_ANSI_COLORING && b;
    //     formatter.setAnsiColoringEnabled(b);
    //     return this;
    // }

    public ConsoleLogger setPrefixEveryLine(boolean b) {
        prefixEveryLine = b;
        return this;
    }

    public ConsoleLogger setShowProblemCodes(boolean b) {
        showProblemCodes = b;
        return this;
    }

    //
    //
    //

    /// Reports a Diagnostic to the console and the line consumer if they are enabled.
    protected void writeString(Diagnostic diagnostic) {


        // ReportWriter writer = writers[diagnostic.getLevel().ordinal()];
        // if (writer == null) {
        //     return;
        // }

        Level level = diagnostic.getLevel();
        PrintStream stream = (level == Level.ERROR && systemErrorEnabled) ? System.err : System.out;
        // PrintWriter writer = new PrintWriter(stream);

        String[] lines = formatter.format(diagnostic).split("\n");
        for (int i = 0; i < lines.length; i++) {
            displayLine(stream, level, lines[i], i);
        }

        // if ((diagnostic.getCause() != null || diagnostic.getStackTrace() != null) && stackTraceEnabled) {
        //     if (diagnostic.getCause() != null) {
        //         writeStackTrace(level, stream, diagnostic.getStackTrace(), diagnostic.getCause());
        //     } else if (diagnostic.getStackTrace() != null) {
        //         writeStackTrace(level, stream, diagnostic.getStackTrace(), null);
        //     }
        // }
    }

    protected void displayLine(PrintStream stream, Level level, String msgLine, int msgLineNumber) {
        boolean indented = msgLine.startsWith(" ");

        if (indented && ansiColoringEnabled) {
            stream.print(TermUtils.ANSI_GREEN);
            stream.print(msgLine);
            stream.print(TermUtils.ANSI_RESET);
        } else {
            stream.print(msgLine);
        }
        stream.print("\n");
        if (flushEnabled) {
            stream.flush();
        }
    }

    // private void writeStackTrace(Level level, PrintStream stream, StackTraceElement[] stackTrace, Throwable t) {

    //     for (StackTraceElement frame : stackTrace) {
    //         String msgLine = String.format(
    //             "  at %s.%s(%s:%d)",
    //             frame.getClassName(),
    //             frame.getMethodName(),
    //             frame.getFileName(),
    //             frame.getLineNumber()
    //         );
    //         // writer.logLine(msgLine);
    //         // writer.displayLine(msgLine, 1);

    //         if (prefixEveryLine) {
    //             stream.print("[");
    //             if (ansiColoringEnabled) {
    //                 String ansiCode = levelColors[level.ordinal()];
    //                 if (ansiCode != null) {
    //                     stream.print(ansiCode);
    //                     stream.print(level.getLogPrefix());
    //                     stream.print(TermUtils.ANSI_RESET);
    //                 } else {
    //                     stream.print(level.getLogPrefix());
    //                 }
    //             } else {
    //                 stream.print(level.getLogPrefix());
    //             }
    //             stream.print("] ");
    //         } else {
    //             stream.print("        ");
    //         }

    //         stream.print(msgLine);
    //     }
    // }
}
