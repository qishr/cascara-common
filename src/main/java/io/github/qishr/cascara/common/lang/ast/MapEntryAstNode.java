package io.github.qishr.cascara.common.lang.ast;

import java.util.List;
import java.util.Map;

// K The type of the key (usually String/ScalarAstNode)
// V The type of the value node
/// Represents the structural pairing of a key and a value in a Map.
public interface MapEntryAstNode<K, V extends AstNode> extends Map.Entry<K,V>, AstNode {

    /// Returns the key corresponding to this entry.
    K getKey();

    default String getKeyString() {
        K key = getKey();
        if (key instanceof String string) {
            return string;
        } else if (key instanceof ScalarAstNode scalar) {
            return scalar.asString();
        } else {
            return key.toString();
        }
    }

    /// Returns the value corresponding to this entry.
    V getValue();

    /// Replaces the value corresponding to this entry with the specified value (optional operation).
    MapEntryAstNode<K,V> setRaw(V value);

    // An unmodifiable list of the child entries.
    @Override
    default List<V> getChildren() {
        // TODO: This doesn't look good...
        // return List.of(getKey(), getValue());
        return List.of(getValue());
    }
}

