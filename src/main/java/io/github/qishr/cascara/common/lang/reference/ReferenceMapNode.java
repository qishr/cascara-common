package io.github.qishr.cascara.common.lang.reference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.qishr.cascara.common.lang.ast.*;


public final class ReferenceMapNode extends ReferenceNode implements MapAstNode<ReferenceNode, ReferenceMapEntryNode> {

    private List<ReferenceMapEntryNode> entries = new ArrayList<>();

    @Override
    public boolean containsKey(ReferenceNode key) {
        return getEntry(key) != null;
    }

    @Override
    public ReferenceNode get(ReferenceNode key) {
        ReferenceMapEntryNode value = getEntry(key);
        return value == null ? null : value.getValue();
    }

    @Override
    public ReferenceMapEntryNode getEntry(ReferenceNode key) {
        for (ReferenceMapEntryNode entry : entries) {
            if (entry.getKey().equals(key)) return entry;
        }
        return null;
    }

    @Override
    public List<ReferenceMapEntryNode> getEntries() {
        return entries;
    }

    @Override
    public Set<ReferenceNode> keySet() {
        return Set.copyOf(entries.stream().map(e -> e.getKey()).toList());
    }

    @Override
    public ReferenceMapNode put(ReferenceNode key, ReferenceNode value) {
        for (ReferenceMapEntryNode entry : entries) {
            if (entry.getKey().equals(key)) {
                entry.setRaw(value);
                return this;
            }
        }
        entries.add(new ReferenceMapEntryNode(key, value));
        return this;
    }

    @Override
    public ReferenceMapNode remove(ReferenceNode key) {
        entries.remove(key);
        return this;
    }

    @Override
    public boolean containsKey(String key) {
        for (ReferenceMapEntryNode entry : entries) {
            if (entry.getKey() instanceof ReferenceScalarNode scalar && key.equals(scalar.asString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ReferenceMapNode put(String key, String value) {
        ReferenceScalarNode scalarValue = new ReferenceScalarNode(value);
        put(key, scalarValue);
        return this;
    }

    @Override
    public ReferenceMapNode put(String key, ReferenceNode value) {
        for (ReferenceMapEntryNode entry : entries) {
            ReferenceNode kNode = entry.getKey();
            // Check if the existing key's string value matches the requested key
            if (kNode instanceof ReferenceScalarNode scalar && key.equals(scalar.asString())) {
                entry.setRaw(value);
                return this;
            }
        }
        // Only if not found, create the new entry
        ReferenceNode keyNode = new ReferenceScalarNode(key);
        entries.add(new ReferenceMapEntryNode(keyNode, value));
        return this;
    }

    @Override
    public ReferenceMapNode remove(String key) {
        Iterator<ReferenceMapEntryNode> it = entries.iterator();
        while (it.hasNext()) {
            ReferenceMapEntryNode entry = it.next();
            AstNode k = entry.getKey();
            if (k instanceof ScalarAstNode scalar && scalar.asString().equals(key)) {
                it.remove();
                return this;
            }
        }
        return this;
    }

    @Override
    public ReferenceNode get(String key) {
        if (key == null) return null;
        for (ReferenceMapEntryNode entry : entries) {
            ReferenceNode kNode = entry.getKey();
            String entryKey = null;
            if (kNode instanceof ReferenceScalarNode scalar) {
                entryKey = scalar.asString();
            } else {
                entryKey = kNode.toString();
            }

            if (key.equals(entryKey)) {
                ReferenceNode val = entry.getValue();
                return val;
            }
        }
        return null;
    }

    @Override
    public List<ReferenceMapEntryNode> getChildren() {
        return entries;
    }

    @Override
    public List<CommentAstNode> getComments() {
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }

    @Override
    public ReferenceMapNode getMap(String key) {
        throw new UnsupportedOperationException("Unimplemented method 'getMap'");
    }

    @Override
    public ReferenceSequenceNode getSequence(String key) {
        throw new UnsupportedOperationException("Unimplemented method 'getSequence'");
    }

    @Override
    public Set<ReferenceMapEntryNode> entrySet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'entrySet'");
    }

    @Override
    public List<ReferenceNode> values() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'values'");
    }
}
