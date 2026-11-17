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

import java.util.function.Consumer;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public interface DiagnosticReporter extends Reporter {

    /// Gets a `GlobalReporter` instance for the specifie class.
    /// @param clazz the class to get a `GlobalReporter` instance for.
    static Reporter forClass(Class<?> clazz) {
        return GlobalReporter.forClass(clazz);
    }

    /// Sets the level of output when logging directly to the console.
    Reporter setLevel(Level level);

    @Experimental
    Reporter setLineConsumer(Consumer<String> logger);

    /// Registers a consumer to receive all diagnostics processed by this reporter.
    /// This includes debugging info, trace states, warnings, and error diagnostics.
    ///
    /// @param consumer The consumer that processes each produced [Diagnostic].
    @Experimental
    Reporter addDiagnosticConsumer(Consumer<Diagnostic> consumer);

    /// Unregisters a consumer of diagnostic reports.
    ///
    /// @param consumer The consumer that processes each produced [Diagnostic].
    @Experimental
    Reporter removeDiagnosticConsumer(Consumer<Diagnostic> consumer);

    /// Registers a consumer to receive only problem-level diagnostics.
    /// This collector is filtered to intercept only `Level.WARN` and `Level.ERROR` items.
    ///
    /// @param consumer The consumer that processes problem [Diagnostic] objects.
    @Experimental
    Reporter setProblemConsumer(Consumer<Diagnostic> consumer);

    // /// Checks whether any active consumer is collecting problems.
    // ///
    // /// This can be used as an optimization flag by sub-parsers or AST-walkers
    // /// to skip expensive location token captures or contextual allocations if nobody
    // /// is actively listening for error diagnostics.
    // ///
    // /// @return `true` if warnings or errors are being collected, otherwise `false`.
    // boolean collectsProblems();

    // boolean isSilent();

    // boolean reportsDebug();

    // boolean reportsTrace();

    // Level getLevel();

    // ReportWriter getWriter(Diagnostic.Level level);
}