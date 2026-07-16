package io.github.qishr.cascara.common.lang.type;

import java.util.SequencedCollection;

/// Based on https://json-schema.org/draft-04/json-schema-core#rfc.section.3.5
public enum PrimitiveType {
    ANY,
    ARRAY,
    BOOLEAN,
    INTEGER,
    NUMBER,
    NULL,
    OBJECT,
    STRING;

    public static PrimitiveType fromString(String type) {
        try {
            return valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OBJECT; // Default fallback
        }
    }

    /// Return the JSON Schema type name
    public String asString() {
        return toString().toLowerCase();
    }

    public static PrimitiveType of(Object jvmType) {
        if (jvmType instanceof String || jvmType instanceof Character) {
            return STRING;
        }
        else if (jvmType instanceof Integer || jvmType instanceof Long) {
            return INTEGER;
        }
        else if (jvmType instanceof Double || jvmType instanceof Float) {
            return NUMBER;
        }
        else if (jvmType instanceof Boolean) {
            return BOOLEAN;
        }
        if (jvmType instanceof SequencedCollection) {
            return ARRAY;
        }
        else if (jvmType == null) {
            return NULL;
        } else {
            return OBJECT;
        }
    }
}
