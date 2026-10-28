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
import io.github.qishr.cascara.common.diagnostic.format.ColoredLogFormatter;
import io.github.qishr.cascara.common.diagnostic.format.FormatOptions;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatter;
import io.github.qishr.cascara.common.diagnostic.format.PlainLogFormatter;
import io.github.qishr.cascara.common.util.JreUtils;

@Experimental
public class ConsoleLogger implements Logger {
    private LogFormatter formatter;
    private FormatOptions formatOptions;

    private boolean flushEnabled = false;
    private boolean systemErrorEnabled = false;
    private boolean ansiColoringEnabled = true;

    public ConsoleLogger() {
        formatOptions = new FormatOptions();
        init();
    }

    public ConsoleLogger(FormatOptions formatOptions) {
        this.formatOptions = formatOptions;
        init();
    }

    @SingletonInitializer
    private void init() {
        boolean console = (System.console() != null);
        boolean gradle = JreUtils.isRunningViaGradle();
        boolean eclipse = JreUtils.isRunningViaEclipse();
        ansiColoringEnabled = console||gradle||eclipse;
        configureFormatter();
    }

    @Override
    public ConsoleLogger log(Diagnostic diagnostic) {
        writeString(diagnostic);
        return this;
    }

    @Override
    public ConsoleLogger flush() {
        throw new UnimplementedMethodException();
    }

    //
    // Options
    //

    public ConsoleLogger setFormatter(LogFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    public ConsoleLogger setFormatOptions(FormatOptions options) {
        // This is required as loggers that are services can't have options
        // passed into their constructor.
        formatOptions = options;
        formatter.setOptions(options);
        return this;
    }

    public ConsoleLogger setImmediateFlushEnabled(boolean b) {
        flushEnabled = b;
        return this;
    }

    public ConsoleLogger setSystemErrorEnabled(boolean b) {
        systemErrorEnabled = b;
        return this;
    }

    public ConsoleLogger setAnsiColoringEnabled(boolean b) {
        ansiColoringEnabled = b;
        configureFormatter();
        return this;
    }

    //
    //
    //

    private void configureFormatter() {
        if (ansiColoringEnabled) {
            formatter = new ColoredLogFormatter(formatOptions);
        } else {
            formatter = new PlainLogFormatter(formatOptions);
        }
        // formatter.setOptions(formatOptions);
    }

    /// Reports a Diagnostic to the console and the line consumer if they are enabled.
    protected void writeString(Diagnostic diagnostic) {
        Level level = diagnostic.getLevel();
        PrintStream stream = (level == Level.ERROR && systemErrorEnabled)
            ? System.err
            : System.out;

        stream.print(formatter.format(diagnostic));
        if (flushEnabled) {
            stream.flush();
        }
    }
}
