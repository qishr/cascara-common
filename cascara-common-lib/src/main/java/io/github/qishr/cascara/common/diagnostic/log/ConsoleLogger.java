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
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.format.ColoredLogFormatter;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatConfig;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatter;
import io.github.qishr.cascara.common.diagnostic.format.PlainLogFormatter;

@Experimental
public class ConsoleLogger implements Logger {
    private LogFormatter formatter;
    private LogFormatConfig formatConfig;
    private ConsoleLoggerConfig config = ConsoleLoggerConfig.get();
    private boolean ansiColoringEnabled = config.isAnsiColoringEnabled();

    public ConsoleLogger() {
        formatConfig = LogFormatConfig.get();
        init();
    }

    private void init() {
        initializeFormatter();
    }

    @Override
    public ConsoleLogger log(Diagnostic diagnostic) {
        writeString(diagnostic);
        return this;
    }

    @Override
    public ConsoleLogger flush() {
        if (config.isSystemErrorEnabled()) {
            System.err.flush();
        }
        System.out.flush();
        return this;
    }

    //
    // Config
    //

    public ConsoleLoggerConfig getConfig() {
        return config;
    }

    public ConsoleLogger setConfig(ConsoleLoggerConfig config) {
        this.config = config;
        return this;
    }

    public ConsoleLogger setFormatter(LogFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    public LogFormatConfig getFormatConfig() {
        return formatConfig;
    }

    /// Sets the format configuration for this logger.
    /// This allows LocalReporter to use different formatting
    /// from GlobalReporter, for example.
    public ConsoleLogger setFormatConfig(LogFormatConfig config) {
        formatConfig = config;
        formatter.setConfig(config);
        return this;
    }

    //
    //
    //

    private void initializeFormatter() {
        if (config.isAnsiColoringEnabled()) {
            formatter = new ColoredLogFormatter(formatConfig);
        } else {
            formatter = new PlainLogFormatter(formatConfig);
        }
    }

    /// Reports a Diagnostic to the console and the line consumer if they are enabled.
    protected void writeString(Diagnostic diagnostic) {
        Level level = diagnostic.getLevel();
        PrintStream stream = (level == Level.ERROR && config.isSystemErrorEnabled())
            ? System.err
            : System.out;

        if (ansiColoringEnabled != config.isAnsiColoringEnabled()) {
            initializeFormatter();
            ansiColoringEnabled = config.isAnsiColoringEnabled();
        }

        stream.print(formatter.format(diagnostic));
        if (config.isImmediateFlushEnabled()) {
            stream.flush();
        }
    }
}
