package io.github.qishr.cascara.common.diagnostic;

import java.io.PrintStream;
import java.io.PrintWriter;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

public interface LocalizableException {

    /// Returns a diagnostic error code for the error message.
	DiagnosticCode getCode();

    /// Returns the details, if any, to be used in formatting the error message.
	Object[] getDetails();

    /// Returns a localized, formatted error message.
    String getLocalizedMessage();

    /// Returns a localized, formatted error message.
    String getMessage();

    Throwable getCause();

    String toString();

    void printStackTrace();

    void printStackTrace(PrintStream s);

    void printStackTrace(PrintWriter s);

    StackTraceElement[] getStackTrace();

}
