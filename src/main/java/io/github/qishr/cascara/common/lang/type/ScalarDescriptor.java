package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public interface ScalarDescriptor<T> extends TypeDescriptor<T> {
    /// Converts the `text` into the JVM type specified by this TypeDescriptor.
    T toJvmType(String text);

    /// Converts the JVM type specified by this TypeDescriptor into a Primitive.
    Primitive toPrimitive(T jvmInstance);

    /// Retuns the JSON Schema `format` used by this TypeDescriptor.
    String getFormat();

    /// Returns the JSON Schema `contentEncoding` use by this TypeDescriptor.
    String getContentEncoding();

    @Override
    default void populateSchema(MapAstNode<?,?> node) {
        // Automatically inject the core properties every scalar might declare.
        node.put("type", getSchemaType());

        String format = getFormat();
        if (format != null && !format.isEmpty()) {
            node.put("format", format);
        }

        String encoding = getContentEncoding();
        if (encoding != null && !encoding.isEmpty()) {
            node.put("contentEncoding", encoding);
        }
    }
}
