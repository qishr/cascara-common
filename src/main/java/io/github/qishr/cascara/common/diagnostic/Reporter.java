package io.github.qishr.cascara.common.diagnostic;

import java.io.Writer;
import java.net.URI;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

public interface Reporter {

    /// Creates a new `Reporter` with the current settings,
    /// specifically for the specified class.
    /// Typically used for global logging by modules.
    Reporter forClass(Class<?> clazz);

    /// Sets the level of output when logging directly to the console.
    Reporter setLevel(Level level);

    // Reporter setWriter(Writer writer);

    Reporter setStringWriter(ReportStringWriter stringWriter);

    Reporter setDiagnosticWriter(ReportDiagnosticWriter diagnosticWriter);

    Reporter setCollector(ReportCollector collector);

    /// Reports an trace message through the reporter.
    /// @param m The message to report.
    void trace(Object... m);

    /// Reports an debug message through the reporter.
    /// @param m The message to report.
    void debug(Object... m);

    /// Reports an informational message through the reporter.
    /// @param m The message to report.
    void info(Object... m);

    /// Reports a warning message including location information.
    /// @param m The warning message to report.
    void warn(Object... m);

    /// Reports an error message including location information.
    /// @param m The error message to report.
    void error(Object... m);

    void infoAt(int line, int column, URI uri, Object... m);
    void warnAt(int line, int column, URI uri, Object... m);
    void errorAt(int line, int column, URI uri, Object... m);
}