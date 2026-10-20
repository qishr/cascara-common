// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

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
    // );


        // TODO: A JDK21 way of telling if output is being redirected
    // protected static boolean isRedirected = (System.out != System.out.getClass().getField("holder").get(null));

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

    public ConsoleLogger() {
        // try {
        //     boolean isRedirected = (System.out != System.out.getClass().getField("holder").get(null));
        // } catch (IllegalArgumentException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // } catch (IllegalAccessException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // } catch (NoSuchFieldException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }

    }

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

        String[] lines = formatter.format(diagnostic, stackTraceEnabled).split("\n");
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
