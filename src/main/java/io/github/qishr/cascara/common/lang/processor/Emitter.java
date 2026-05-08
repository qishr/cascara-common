package io.github.qishr.cascara.common.lang.processor;

import io.github.qishr.cascara.common.util.ContentType;

/// A generic interface for emitting structured text.
public interface Emitter extends Processor {
    /// Writes a literal scalar value.
    void emitScalar(String value);

    /// Signals the start of a mapping/object.
    void emitMapStart();

    /// Signals the end of a mapping/object.
    void emitMapEnd();

    /// Signals the start of a sequence/list.
    void emitSequenceStart();

    /// Signals the end of a sequence/list.
    void emitSequenceEnd();

    /// Writes a separator between key and value.
    void emitPropertySeparator();

    /// Writes a separator between items in a collection.
    void emitItemSeparator();

    /// Manages white-space and indentation.
    void emitNewLine();
    void indent();
    void dedent();

    /// Returns the final produced string.
    String getOutput();

    ContentType getContentType();
}