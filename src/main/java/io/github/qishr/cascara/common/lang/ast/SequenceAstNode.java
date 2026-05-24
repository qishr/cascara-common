package io.github.qishr.cascara.common.lang.ast;

import java.util.Iterator;
import java.util.List;

public interface SequenceAstNode<T extends AstNode> extends AstNode, Iterable<T> {
    int size();
    void clear();
    SequenceAstNode<T> add(T node);
    SequenceAstNode<T> remove(T node);
    SequenceAstNode<T> remove(int index);
    T get(int index);
    List<T> getElements();
    Iterator<T> iterator();
}
