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
            String pattern = code.getMessage().replace("'", "''");
            return MessageFormat.format(pattern, details);
        } catch (IllegalArgumentException e) {
            return String.format(FORMATTING_ERROR, code.getCode(), code.getMessage());
        }
    };
}