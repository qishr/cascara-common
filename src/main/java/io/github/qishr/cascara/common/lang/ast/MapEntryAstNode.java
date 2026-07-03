package io.github.qishr.cascara.common.lang.ast;

import java.util.List;
import java.util.Map;

// K The type of the key (usually String/ScalarAstNode)
// V The type of the value node
/// Represents the structural pairing of a key and a value in a Map.
public interface MapEntryAstNode<V extends AstNode> extends Map.Entry<V,V>, AstNode {

    /// Returns the key corresponding to this entry.
    V getKey();

    /// Returns the value corresponding to this entry.
    V getValue();

    /// Replaces the value corresponding to this entry with the specified value (optional operation).
    MapEntryAstNode<V> setRaw(V value);

    // An unmodifiable list of the child entries.
    @Override
    default List<V> getChildren() {
        return List.of(getKey(), getValue());
    }
}

