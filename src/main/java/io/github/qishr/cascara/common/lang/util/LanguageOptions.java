package io.github.qishr.cascara.common.lang.util;

/// Base class for all language-specific configuration.
/// T is the specific implementation type for fluent chaining.
public abstract class LanguageOptions<T extends LanguageOptions<T>> {
    protected int indentSize = 2;

    /// Sets the number of spaces/tabs used for indentation.
    @SuppressWarnings("unchecked")
    public T setIndentSize(int size) {
        this.indentSize = size;
        return (T) this;
    }

    public int getIndentSize() { return indentSize; }
}