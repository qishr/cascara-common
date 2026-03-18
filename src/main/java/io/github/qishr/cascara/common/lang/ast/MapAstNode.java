package io.github.qishr.cascara.common.lang.ast;

import java.util.List;
import java.util.Set;

public interface MapAstNode<T extends AstNode, E extends MapEntryAstNode<T>> extends AstNode {
    boolean containsKey(T key);
    T get(T key);
    E getEntry(T key);
    List<E> getEntries();
    Set<T> keys();
    void put(T key, T value);
    void remove(T key);

    @Override
    default List<E> getChildren() {
        return getEntries();
    }

    //
    // Convenience Methods for String keys
    //

    boolean containsKey(String key);
    void put(String key, T value);
    void remove(String key);
    T get(String key);

    MapAstNode<T,E> getMap(String key);
    SequenceAstNode<T> getSequence(String key);

    /// @return Returns the string value of the map entry, or null if it doesn't exist.
    default String getString(String key) {
        T node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.getString() : null;
    }

    default int getInteger(String key, int defaultValue) {
        T node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.getInteger() : defaultValue;
    }

    default double getDouble(String key, double defaultValue) {
        T node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.getDouble() : defaultValue;
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        T node = get(key);
        return (node instanceof ScalarAstNode scalar) ? scalar.getBoolean() : defaultValue;
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
}
