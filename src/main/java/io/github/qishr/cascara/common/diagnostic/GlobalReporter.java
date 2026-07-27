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


package io.github.qishr.cascara.common.diagnostic;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public class GlobalReporter extends AbstractReporter<GlobalReporter> {
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final GlobalReporter globalInstance = new GlobalReporter();

    private static final Map<String,GlobalReporter> classInstances = new HashMap<>();

    private GlobalReporter(String source) {
        this.source = source;
        this.level = globalInstance.level;
    }

    private GlobalReporter() {
        // Nothing to see here
    }

    /// {@inheritDoc}
    @Override
    protected GlobalReporter self() { return this; }

    public static GlobalReporter globalInstance() {
        return globalInstance;
    }

    public static GlobalReporter forClass(Class<?> clazz) {
        return forSource(clazz.getSimpleName());
    }

    public static GlobalReporter forSource(String source) {
        GlobalReporter reporter = classInstances.get(source);
        if (reporter == null) {
            reporter = new GlobalReporter(source);
            classInstances.put(source, reporter);
        }
        return reporter;
    }

    @Override
    public GlobalReporter setLevel(Level level) {
        this.level = level;
        return this;
    }

    @Override
    public GlobalReporter setDiagnosticCollector(Consumer<Diagnostic> collector) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDiagnosticWriter in GlobalReporter may only be called on the global instance.");
        }
        super.setDiagnosticCollector(collector);
        return this;
    }

    @Override
    public GlobalReporter setProblemCollector(Consumer<Diagnostic> collector) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setCollector in GlobalReporter may only be called on the global instance.");
        }
        super.setProblemCollector(collector);
        return this;
    }

    public GlobalReporter setSystemOutputEnabled(boolean b) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDisableSystemOutput in GlobalReporter may only be called on the global instance.");
        }
        super.setSystemOutputEnabled(b);
        return this;
    }

    public GlobalReporter setFlushEnabled(boolean b) {
        if (this != globalInstance) {
            throw new UnsupportedOperationException("The method setDisableFlush in GlobalReporter may only be called on the global instance.");
        }
        super.setFlushEnabled(b);
        return this;
    }

    //
    //
    //

    @Override
    protected Consumer<Diagnostic> getDiagnosticCollector() {
        return this == globalInstance ? diagnosticCollector : globalInstance.getDiagnosticCollector();
    }

    @Override
    protected Consumer<Diagnostic> getProblemCollector() {
        return this == globalInstance ? problemCollector : globalInstance.getProblemCollector();
    }

    @Override
    protected Consumer<String> getStringWriter() {
        return this == globalInstance ? stringWriter : globalInstance.getStringWriter();
    }

    @Override
    protected boolean isSystemOutputEnabled() {
        return this == globalInstance ? systemOutputEnabled : globalInstance.isSystemOutputEnabled();
    }

    @Override
    protected boolean isFlushEnabled() {
        return this == globalInstance ? flushEnabled : globalInstance.isFlushEnabled();
    }

    @Override
    protected boolean isStackTraceEnabled() {
        return this == globalInstance ? stackTraceEnabled : globalInstance.isStackTraceEnabled();
    }

    @Override
    protected void writeString(Diagnostic diagnostic) {
        writeString (
            diagnostic.getCause(),
            diagnostic.getLevel(),
            formatString(diagnostic)
        );
    }

    private String formatString(Diagnostic diagnostic) {
        if (diagnostic.getUri() == null) {
            if (diagnostic.getLine() > 0) {
                return String.format(
                    "[%5s] [%s] [%s] %s at line %d\n",
                    diagnostic.getLevel(),
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getMessage(),
                    diagnostic.getLine()
                );
            } else {
                return String.format(
                    "[%5s] [%s] [%s] %s\n",
                    diagnostic.getLevel(),
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getMessage()
                );
            }
        } else {
            if (diagnostic.getLine() > 0) {
                return String.format(
                    "[%5s] [%s] [%s] %s at %s:%d\n",
                    diagnostic.getLevel(),
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getMessage(),
                    diagnostic.getUri(),
                    diagnostic.getLine()
                );
            } else {
                return String.format(
                    "[%5s] [%s] [%s] %s in file %s\n",
                    diagnostic.getLevel(),
                    diagnostic.getTimestamp().format(TIME_FORMAT),
                    diagnostic.getSource(),
                    diagnostic.getMessage(),
                    diagnostic.getUri()
                );
            }
        }
    }
}
