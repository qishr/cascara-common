package io.github.qishr.cascara.common.lang.reference;

import io.github.qishr.cascara.common.lang.ast.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class ReferenceSequenceNode extends ReferenceNode implements SequenceAstNode<ReferenceNode> {

    private final List<ReferenceNode> elements = new ArrayList<>();

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public ReferenceSequenceNode clear() {
        elements.clear();
        return this;
    }

    @Override
    public ReferenceSequenceNode add(ReferenceNode node) {
        elements.add(node);
        return this;
    }

    @Override
    public ReferenceSequenceNode remove(ReferenceNode node) {
        elements.remove(node);
        return this;
    }

    @Override
    public ReferenceSequenceNode remove(int index) {
        elements.remove(index);
        return this;
    }

    @Override
    public ReferenceNode get(int index) {
        return elements.get(index);
    }

    @Override
    public List<ReferenceNode> getElements() {
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
    public Iterator<ReferenceNode> iterator() {
        return new SequenceIterator<ReferenceNode>(this);
    }

    static class SequenceIterator<T> implements Iterator<ReferenceNode> {
        ReferenceSequenceNode list;
        int currentIndex = 0;

        // initialize pointer to head of the list for iteration
        public SequenceIterator(ReferenceSequenceNode list) {
            this.list = list;
        }

        // returns false if next element does not exist
        public boolean hasNext() {
            return currentIndex < list.size();
        }

        // return current data and update pointer
        public ReferenceNode next() {
            ReferenceNode data = list.get(currentIndex++);
            return data;
        }

        // implement if needed
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
