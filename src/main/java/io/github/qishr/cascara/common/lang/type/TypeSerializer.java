package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.exception.SerializerException;

public interface TypeSerializer<T> extends TypeDescriptor<T> {
    /// Transforms a concrete Java object into its structural AST representation.
    ///
    /// @param value    The live runtime object instance to serialize.
    /// @return         The matching structural AstNode graph.
    AstNode serialize(T value) throws SerializerException;

    /// Deserializes an AST node into a strongly-typed Java object.
    ///
    /// @param jvmInstance        The structural AST node being parsed (e.g., YamlScalarNode, YamlMapNode).
    /// @return            The fully constructed Java object instance.
    /// @throws SerializerException If the node structure violates the type constraints.
    public abstract T deserialize(AstNode jvmInstance) throws SerializerException;
}
