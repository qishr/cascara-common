package io.github.qishr.cascara.common.lang.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.qishr.cascara.common.lang.ast.*;


public final class SimpleMapNode extends SimpleNode implements MapAstNode<SimpleNode, SimpleMapEntryNode> {

    private List<SimpleMapEntryNode> entries = new ArrayList<>();

    @Override
    public boolean containsKey(SimpleNode key) {
        return getEntry(key) != null;
    }

    @Override
    public SimpleNode get(SimpleNode key) {
        SimpleMapEntryNode value = getEntry(key);
        return value == null ? null : value.getValue();
    }

    @Override
    public SimpleMapEntryNode getEntry(SimpleNode key) {
        for (SimpleMapEntryNode entry : entries) {
            if (entry.getKey().equals(key)) return entry;
        }
        return null;
    }

    @Override
    public List<SimpleMapEntryNode> getEntries() {
        return entries;
    }

    @Override
    public Set<SimpleNode> keySet() {
        return Set.copyOf(entries.stream().map(e -> e.getKey()).toList());
    }

    @Override
    public SimpleMapNode put(SimpleNode key, SimpleNode value) {
        for (SimpleMapEntryNode entry : entries) {
            if (entry.getKey().equals(key)) {
                entry.setValue(value);
                return this;
            }
        }
        entries.add(new SimpleMapEntryNode(key, value));
        return this;
    }

    @Override
    public void remove(SimpleNode key) {
        entries.remove(key);
    }

    @Override
    public boolean containsKey(String key) {
        for (SimpleMapEntryNode entry : entries) {
            if (entry.getKey() instanceof SimpleScalarNode scalar && key.equals(scalar.getString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SimpleMapNode put(String key, String value) {
        SimpleScalarNode scalarValue = new SimpleScalarNode(value);
        put(key, scalarValue);
        return this;
    }

    @Override
    public SimpleMapNode put(String key, SimpleNode value) {
        for (SimpleMapEntryNode entry : entries) {
            SimpleNode kNode = entry.getKey();
            // Check if the existing key's string value matches the requested key
            if (kNode instanceof SimpleScalarNode scalar && key.equals(scalar.getString())) {
                entry.setValue(value);
                return this;
            }
        }
        // Only if not found, create the new entry
        SimpleNode keyNode = new SimpleScalarNode(key);
        entries.add(new SimpleMapEntryNode(keyNode, value));
        return this;
    }

    @Override
    public void remove(String key) {
        Iterator<SimpleMapEntryNode> it = entries.iterator();
        while (it.hasNext()) {
            SimpleMapEntryNode entry = it.next();
            AstNode k = entry.getKey();
            if (k instanceof ScalarAstNode scalar && scalar.getString().equals(key)) {
                it.remove();
                return;
            }
        }
    }

    @Override
    public SimpleNode get(String key) {
        if (key == null) return null;
        for (SimpleMapEntryNode entry : entries) {
            SimpleNode kNode = entry.getKey();
            String entryKey = null;
            if (kNode instanceof SimpleScalarNode scalar) {
                entryKey = scalar.getString();
            } else {
                entryKey = kNode.toString();
            }

            if (key.equals(entryKey)) {
                SimpleNode val = entry.getValue();
                return val;
            }
        }
        return null;
    }

    @Override
    public List<SimpleMapEntryNode> getChildren() {
        return entries;
    }

    @Override
    public List<CommentAstNode> getComments() {
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }

    @Override
    public SimpleMapNode getMap(String key) {
        throw new UnsupportedOperationException("Unimplemented method 'getMap'");
    }

    @Override
    public SimpleSequenceNode getSequence(String key) {
        throw new UnsupportedOperationException("Unimplemented method 'getSequence'");
    }

    @Override
    public Set<SimpleMapEntryNode> entrySet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'entrySet'");
    }

    @Override
    public Collection<SimpleNode> values() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'values'");
    }
}
