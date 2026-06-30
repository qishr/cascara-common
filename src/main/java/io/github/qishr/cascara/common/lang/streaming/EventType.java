package io.github.qishr.cascara.common.lang.streaming;

import io.github.qishr.cascara.common.lang.annotation.Experimental;

@Experimental
public enum EventType {
    START_DOCUMENT,

    END_DOCUMENT,

    /// Maps to Map/Object entry boundaries
    START_OBJECT,

    END_OBJECT,

    /// Maps to List/Sequence boundaries
    START_ARRAY,

    END_ARRAY,

    /// Keys
    FIELD_NAME,

    /// String, number, boolean, null
    VALUE_SCALAR,

    COMMENT
}