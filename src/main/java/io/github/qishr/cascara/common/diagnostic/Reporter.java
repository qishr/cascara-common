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

import java.net.URI;
import java.util.function.Consumer;

import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;
import io.github.qishr.cascara.common.lang.token.Token;

public interface Reporter {

    Reporter setLineConsumer(Consumer<String> logger);

    /// Sets the level of output when logging directly to the console.
    Reporter setLevel(Level level);

    /// Registers a collector to receive all diagnostics processed by this reporter.
    /// This includes debugging info, trace states, warnings, and error diagnostics.
    ///
    /// @param collector The consumer that processes each produced [Diagnostic].
    Reporter setDiagnosticConsumer(Consumer<Diagnostic> collector);

    /// Registers a specialized collector to receive only problem-level diagnostics.
    /// This collector is filtered to intercept only `Level.WARN` and `Level.ERROR` items.
    ///
    /// @param collector The consumer that processes problem [Diagnostic] objects.
    Reporter setProblemConsumer(Consumer<Diagnostic> collector);

    /// Checks whether any active listener or collector is tracking problems.
    ///
    /// This can be used as an optimization flag by sub-parsers or AST-walkers
    /// to skip expensive location token captures or contextual allocations if nobody
    /// is actively listening for error diagnostics.
    ///
    /// @return `true` if warnings or errors are being collected, otherwise `false`.
    boolean collectsProblems();

    Level getLevel();

    boolean isSilent();

    ReportWriter getWriter(Diagnostic.Level level);

    //
    // Reporting Methods
    //

    /// Reports an [Exception]
    ///
    /// @param exception The exception to report.
    void error(Exception exception);

    /// Reports a trace message through the reporter.
    /// @param format The format of the message to report.
    /// @param details Arguments referenced by the format specifiers in the format string.
    void trace(String format, Object... details);

    /// Reports a debug message through the reporter.
    /// @param format The format of the message to report.
    /// @param details Arguments referenced by the format specifiers in the format string.
    void debug(String format, Object... details);

    /// Reports an informational message through the reporter.
    /// @param code The code of this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void info(DiagnosticCode code, Object... details);

    /// Reports a warning message including location information.
    /// @param code The code of this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void warn(DiagnosticCode code, Object... details);

    /// Reports an error message including location information.
    /// @param code The code of this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void error(DiagnosticCode code, Object... details);

    /// Reports an error message including location information.
    /// @param cause The cause of this report.
    /// @param code The code of this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void error(Throwable cause, DiagnosticCode code, Object... details);

    /// Reports an informational message anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param code The semantic classification code for this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void infoAt(int line, int column, DiagnosticCode code, Object... details);

    /// Reports a warning anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param code The semantic classification code for this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void warnAt(int line, int column, DiagnosticCode code, Object... details);

    /// Reports an error anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(int line, int column, DiagnosticCode code, Object... details);


    @Experimental
    void errorAt(URI uri, int line, int column, DiagnosticCode code, Object... details);

    @Experimental
    void errorAt(URI uri, Token token, DiagnosticCode code, Object... details);

    @Experimental
    void errorAt(URI uri, Token token, Throwable t, DiagnosticCode code, Object... details);

    /// Reports an error anchored to a resource location by line and column.
    /// Useful when text stream indices are unavailable.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param cause The cause of this report.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(int line, int column, Throwable cause, DiagnosticCode code, Object... details);

    /// Reports an informational message anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void infoAt(int line, int column, int start, int end, DiagnosticCode code, Object... details);

    /// Reports a warning anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param code The semantic classification code for this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void warnAt(int line, int column, int start, int end, DiagnosticCode code, Object... details);

    /// Reports an error anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(int line, int column, int start, int end, DiagnosticCode code, Object... details);

    /// Reports an error anchored to a precise character span within a resource.
    ///
    /// @param line The 1-based line number of the diagnostic.
    /// @param column The 1-based column number of the diagnostic.
    /// @param start The 0-based absolute character index indicating the start of the span.
    /// @param end The 0-based absolute character index indicating the end of the span (exclusive).
    /// @param cause The cause of this report.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(int line, int column, int start, int end, Throwable cause, DiagnosticCode code, Object... details);

    /// Reports an informational message derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void infoAt(Token token, DiagnosticCode code, Object... details);

    /// Reports a warning derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param code The semantic classification code for this warning.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void warnAt(Token token, DiagnosticCode code, Object... details);

    /// Reports an error derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(Token token, DiagnosticCode code, Object... details);

    /// Reports an error derived from the location attributes of a structural token.
    ///
    /// @param token The syntactic [Token] supplying the positional bounds.
    /// @param cause The cause of this report.
    /// @param code The semantic classification code for this error.
    /// @param details Arguments referenced by the format specifiers in the [DiagnosticCode]'s localized format string.
    void errorAt(Token token, Throwable cause, DiagnosticCode code, Object... details);

    boolean reportsDebug();

    boolean reportsTrace();
}