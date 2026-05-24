package io.github.qishr.cascara.common.lang.simple;

import io.github.qishr.cascara.common.lang.ast.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class SimpleSequenceNode extends SimpleNode implements SequenceAstNode<SimpleNode> {

    private final List<SimpleNode> elements = new ArrayList<>();

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Override
    public SimpleSequenceNode add(SimpleNode node) {
        elements.add(node);
        return this;
    }

    @Override
    public SimpleSequenceNode remove(SimpleNode node) {
        elements.remove(node);
        return this;
    }

    @Override
    public SimpleSequenceNode remove(int index) {
        elements.remove(index);
        return this;
    }

    @Override
    public SimpleNode get(int index) {
        return elements.get(index);
    }

    @Override
    public List<SimpleNode> getElements() {
        return elements;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return elements;
    }

    @Override
    public List<CommentAstNode> getComments() {
        return Collections.emptyList();
    }

    /// Returns Iterator instance
    public Iterator<SimpleNode> iterator() {
        return new SequenceIterator<SimpleNode>(this);
    }

    static class SequenceIterator<T> implements Iterator<SimpleNode> {
        SimpleSequenceNode list;
        int currentIndex = 0;

        // initialize pointer to head of the list for iteration
        public SequenceIterator(SimpleSequenceNode list) {
            this.list = list;
        }

        // returns false if next element does not exist
        public boolean hasNext() {
            return currentIndex < list.size();
        }

        // return current data and update pointer
        public SimpleNode next() {
            SimpleNode data = list.get(currentIndex++);
            return data;
        }

        // implement if needed
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
