// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.

package io.github.qishr.cascara.common.lang.plain;

import io.github.qishr.cascara.common.lang.ast.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class PlainSequenceNode extends PlainNode implements SequenceAstNode<AstNode> {

    private final List<AstNode> elements = new ArrayList<>();

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public PlainSequenceNode clear() {
        elements.clear();
        return this;
    }

    @Override
    public PlainSequenceNode add(AstNode node) {
        elements.add(node);
        return this;
    }

    @Override
    public PlainSequenceNode remove(AstNode node) {
        elements.remove(node);
        return this;
    }

    @Override
    public PlainSequenceNode remove(int index) {
        elements.remove(index);
        return this;
    }

    @Override
    public AstNode get(int index) {
        return elements.get(index);
    }

    @Override
    public AstNode getFirst() {
        if (elements.isEmpty()) {
            throw new NoSuchElementException();
        }
        return elements.getFirst();
    }

    @Override
    public AstNode getLast() {
        if (elements.isEmpty()) {
            throw new NoSuchElementException();
        }
        return elements.getLast();
    }

    //
    // Convenience Getters
    //

    @SuppressWarnings("unchecked")
    public MapAstNode<AstNode, AstNode, PlainMapEntryNode> getMap(int i) {
        if (get(i) instanceof MapAstNode map) {
            return map;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public SequenceAstNode<AstNode> getSequence(int i) {
        if (get(i) instanceof SequenceAstNode seq) {
            return seq;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public ScalarAstNode<AstNode> getScalar(int i) {
        if (get(i) instanceof ScalarAstNode scalar) {
            return scalar;
        }
        return null;
    }

    //
    //
    //

    @Override
    public List<AstNode> getElements() {
        return elements;
    }

    @Override
    public List<AstNode> getChildren() {
        return elements;
    }

    @Override
    public List<CommentAstNode> getComments() {
        return Collections.emptyList();
    }

    /// Returns Iterator instance
    @Override
    public Iterator<AstNode> iterator() {
        return new SequenceIterator<AstNode>(this);
    }

    static class SequenceIterator<T> implements Iterator<AstNode> {
        PlainSequenceNode list;
        int currentIndex = 0;

        // initialize pointer to head of the list for iteration
        public SequenceIterator(PlainSequenceNode list) {
            this.list = list;
        }

        // returns false if next element does not exist
        public boolean hasNext() {
            return currentIndex < list.size();
        }

        // return current data and update pointer
        public AstNode next() {
            AstNode data = list.get(currentIndex++);
            return data;
        }

        // implement if needed
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
