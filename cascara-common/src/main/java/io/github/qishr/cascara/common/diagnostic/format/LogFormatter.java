package io.github.qishr.cascara.common.diagnostic.format;

import java.util.Set;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;

/// Formats diagnostic instances into string representations.
///
/// @since cascara.common-1.0.0
public interface LogFormatter {

    /// Configurable fields that can be toggled in formatted output.
    enum Field {
        TIMESTAMP,
        THREAD_ID,
        SOURCE_CLASS,
        LEVEL,
        MESSAGE,
        LOCATION
    }

    /// Formats the given diagnostic into a output line.
    ///
    /// @param diagnostic the diagnostic to format
    /// @return formatted string line
    /// @since cascara.common-1.0.0
    String format(Diagnostic diagnostic);

    /// Configures active fields shown in output.
    ///
    /// @param fields active fields to include
    /// @return this formatter instance
    /// @since cascara.common-1.0.0
    LogFormatter withFields(Set<Field> fields);

    String formatStackTrace(Level level, StackTraceElement[] stackTrace, Throwable t);
}