package io.github.qishr.cascara.common.lang.processor;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.util.LanguageOptions;
import io.github.qishr.cascara.common.service.ServiceProvider;
import io.github.qishr.cascara.common.util.ContentType;

public interface Processor extends ServiceProvider {
    ContentType getContentType();

    /// Sets the reporter for communicating warnings or errors.
    Processor setReporter(Reporter reporter);

    /// Sets the language-specific options (e.g., indentation, Unicode support).
    Processor setOptions(LanguageOptions<?> options);
}
