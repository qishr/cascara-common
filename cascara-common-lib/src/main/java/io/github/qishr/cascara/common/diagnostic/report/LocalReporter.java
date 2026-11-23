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

package io.github.qishr.cascara.common.diagnostic.report;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.format.LogFormatConfig;
import io.github.qishr.cascara.common.diagnostic.log.ConsoleLogger;
import io.github.qishr.cascara.common.diagnostic.log.LineLogger;

public class LocalReporter extends AbstractReporter<LocalReporter> implements DiagnosticReporter {

    /// Consumes diagnostics included in the current Level or more
    /// important, with ERROR being the most important.
    protected Set<Consumer<Diagnostic>> diagnosticConsumers = new HashSet<>();

    /// Consumes ERROR, WARN, and INFO diagnostics.
    protected Consumer<Diagnostic> problemConsumer;

    private boolean consoleOutputEnabled = ReporterConfig.get().isConsoleOutputEnabled();

    public LocalReporter() {
        consoleLogger = new ConsoleLogger().setFormatConfig(formatConfig);
        lineLogger = new LineLogger().setFormatConfig(formatConfig);
    }

    @Override
    protected LocalReporter self() { return this; }

    public String getSource() {
        return source;
    }

    public LocalReporter setSource(String source) {
        this.source = source;
        return this;
    }

    public LocalReporter setFormatConfig(LogFormatConfig config) {
        consoleLogger.setFormatConfig(config);
        lineLogger.setFormatConfig(config);
        return this;
    }

    @Override
    protected boolean isConsoleOutputEnabled() {
        return consoleOutputEnabled;
    }

    /// Controls whether diagnostic logging to console is enabled.
    public LocalReporter setConsoleOutputEnabled(boolean b) {
        consoleOutputEnabled = b;
        return this;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public boolean collectsProblems() {
        return problemConsumer != null;
    }

    @Experimental
    protected Set<Consumer<Diagnostic>> getDiagnosticConsumers() {
        return diagnosticConsumers;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public LocalReporter addDiagnosticConsumer(Consumer<Diagnostic> consumer) {
        diagnosticConsumers.add(consumer);
        return this;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public LocalReporter removeDiagnosticConsumer(Consumer<Diagnostic> consumer) {
        diagnosticConsumers.remove(consumer);
        return this;
    }

    @Experimental
    protected Consumer<Diagnostic> getProblemConsumer() {
        return problemConsumer;
    }

    /// {@inheritDoc}
    @Experimental
    @Override
    public LocalReporter setProblemConsumer(Consumer<Diagnostic> consumer) {
        problemConsumer = consumer;
        return this;
    }

    /// Central reporting method. All other reporting methods call this.
    @Override
    public void report(Diagnostic diagnostic) {
        super.report(diagnostic);

        for (Consumer<Diagnostic> consumer : diagnosticConsumers) {
            consumer.accept(diagnostic);
        }

        if (getProblemConsumer() != null && isProblem(level)) {
            getProblemConsumer().accept(diagnostic);
        }
    }
}
