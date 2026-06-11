package io.github.qishr.cascara.common.diagnostic;

import java.text.MessageFormat;

import io.github.qishr.cascara.common.diagnostic.code.DiagnosticCode;

@FunctionalInterface
public interface DiagnosticLocalizer {
    public static final String FORMATTING_ERROR = "Problem encountered while formatting error with code %s: %s";

    /// Formats the code with dynamic arguments using the environment's current language bundle.
    String format(DiagnosticCode code, Object... details);

    /// A default fail-safe implementation that falls back to standard MessageFormat
    DiagnosticLocalizer DEFAULT = (code, details) -> {
        try {
            return MessageFormat.format(code.getMessage(), details);
        } catch (IllegalArgumentException e) {
            return String.format(FORMATTING_ERROR, code.getCode(), code.getMessage());
        }
    };
}