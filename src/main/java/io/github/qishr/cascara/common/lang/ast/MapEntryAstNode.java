package io.github.qishr.cascara.common.lang.ast;

import java.util.List;

/// Represents the structural pairing of a key and a value in a Map.
/// K The type of the key (usually String/ScalarAstNode)
/// V The type of the value node
public interface MapEntryAstNode<T extends AstNode> extends AstNode {

    /// Returns the key corresponding to this entry.
    T getKey();

    /// Returns the value corresponding to this entry.
    T getValue();

    /// Replaces the value corresponding to this entry with the specified value (optional operation).
    void setRaw(T value);

    @Override
    default List<T> getChildren() {
        return List.of(getKey(), getValue());
    }
}

