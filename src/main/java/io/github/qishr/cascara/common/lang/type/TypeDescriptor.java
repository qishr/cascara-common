package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.service.ServiceProvider;

public interface TypeDescriptor<T> extends ServiceProvider {
    /// Returns the JVM type handled by this TypeDescriptor.
    Class<T> getJvmType();

    /// Returns the JSON Schema type handled by this TypeDescriptor.
    String getSchemaType();

    /// Adds JSON Schema keywords and values to a map representing a
    /// JSON schema for the JSON type handled by this TypeDescriptor.
    void populateSchema(MapAstNode<?,?> node);
}
