package io.github.qishr.cascara.common.lang.ast;

import java.util.List;
import java.util.Set;

public interface MapAstNode<K, V extends AstNode, E extends MapEntryAstNode<K,V>> extends AstNode, Iterable<E> {
    int size();
    boolean isEmpty();
    boolean containsKey(K key);
    V get(K key);
    E getEntry(K key);
    List<E> getEntries();
    Set<E> entrySet();
    Set<K> keySet();
    MapAstNode<K,V,E> put(K key, V value);
    MapAstNode<K,V,E> remove(K key);

    List<V> values();

    @Override
    default List<E> getChildren() {
        return getEntries();
    }

    //
    // Convenience Methods
    //

    boolean containsKey(String key);
    MapAstNode<K,V,E> put(String key, V value);
    MapAstNode<K,V,E> put(String key, String value);
    MapAstNode<K,V,E> remove(String key);
    V get(String key);

    MapAstNode<K,V,E> getMap(String key);
    SequenceAstNode<V> getSequence(String key);

    /// @return Returns the string value of the map entry, or null if it doesn't exist.
    default String getString(String key) {
        V node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.asString() : null;
    }

    default int getInteger(String key, int defaultValue) {
        V node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.asInteger() : defaultValue;
    }

    default double getDouble(String key, double defaultValue) {
        V node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.asDouble() : defaultValue;
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        V node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.asBoolean() : defaultValue;
    }

    default int getInteger(String key) {
        return getInteger(key, 0);
    }

    default double getDouble(String key) {
        return getDouble(key, 0);
    }

    default boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    default String getString(String key, String defaultValue) {
        V value = get(key);
        return (value != null) ? value.asString() : defaultValue;
    }

    default String getAttributeOrDefault(String key, String defaultValue) {
        return getString(key, defaultValue);
    }
}
