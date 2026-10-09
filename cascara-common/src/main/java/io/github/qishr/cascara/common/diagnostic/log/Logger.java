package io.github.qishr.cascara.common.diagnostic.log;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;

/// Contract for outputting formatted log events and system diagnostics.
///
/// @since cascara.common-1.0.0
public interface Logger {

    /// Logs standard diagnostic data.
    ///
    /// @param diagnostic the diagnostic payload to log
    /// @since cascara.common-1.0.0
    void log(Diagnostic diagnostic);

    /// Flushes any buffered log entries to the underlying output sink.
    ///
    /// @since cascara.common-1.0.0
    void flush();
}