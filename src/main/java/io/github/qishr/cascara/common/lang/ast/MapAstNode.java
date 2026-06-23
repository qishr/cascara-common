package io.github.qishr.cascara.common.lang.ast;

import java.util.List;
import java.util.Set;

public interface MapAstNode<T extends AstNode, E extends MapEntryAstNode<T>> extends AstNode {
    boolean containsKey(T key);
    T get(T key);
    E getEntry(T key);
    List<E> getEntries();
    Set<E> entrySet();
    Set<T> keySet();
    MapAstNode<T,E> put(T key, T value);
    MapAstNode<T,E> remove(T key);

    List<T> values();

    @Override
    default List<E> getChildren() {
        return getEntries();
    }

    //
    // Convenience Methods
    //

    boolean containsKey(String key);
    MapAstNode<T,E> put(String key, T value);
    MapAstNode<T,E> put(String key, String value);
    MapAstNode<T,E> remove(String key);
    T get(String key);

    MapAstNode<T,E> getMap(String key);
    SequenceAstNode<T> getSequence(String key);

    /// @return Returns the string value of the map entry, or null if it doesn't exist.
    default String getString(String key) {
        T node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.asString() : null;
    }

    default int getInteger(String key, int defaultValue) {
        T node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.asInteger() : defaultValue;
    }

    default double getDouble(String key, double defaultValue) {
        T node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.asDouble() : defaultValue;
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        T node = get(key);
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
        T value = get(key);
        return (value != null) ? value.asString() : defaultValue;
    }

    default String getAttributeOrDefault(String key, String defaultValue) {
        return getString(key, defaultValue);
    }
}
