package io.github.qishr.cascara.common.lang.reference;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.qishr.cascara.common.lang.ast.*;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;


public final class ReferenceMapNode extends ReferenceNode implements MapAstNode<ReferenceNode, ReferenceMapEntryNode> {

    // private List<ReferenceMapEntryNode> entries = new ArrayList<>();
    // private final LinkedHashMap<ReferenceNode,ReferenceNode> entries = new LinkedHashMap<>();
    private final LinkedHashMap<ReferenceNode,ReferenceMapEntryNode> entriesByKey = new LinkedHashMap<>();

    /// {@inheritDoc}
    @Override
    public boolean isEmpty() {
        return entriesByKey.isEmpty();
    }

    /// {@inheritDoc}
    @Override
    public int size() {
        return entriesByKey.size();
    }

    @Override
    public boolean containsKey(ReferenceNode key) {
        return getEntry(key) != null;
    }

    @Override
    public Set<ReferenceNode> keySet() {
        return entriesByKey.keySet();
    }

    @Override
    public ReferenceNode get(ReferenceNode key) {
        ReferenceMapEntryNode value = getEntry(key);
        return value == null ? null : value.getValue();
    }

    @Override
    public ReferenceMapEntryNode getEntry(ReferenceNode key) {
        return entriesByKey.get(key);
    }

    @Override
    public List<ReferenceMapEntryNode> getEntries() {
        return List.copyOf(entriesByKey.values());
    }

    @Override
    public Set<ReferenceMapEntryNode> entrySet() {
        entriesByKey.entrySet();
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'entrySet'");
    }

    @Override
    public List<ReferenceNode> values() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'values'");
    }

    @Override
    public ReferenceMapNode put(ReferenceNode key, ReferenceNode value) {



        ReferenceMapEntryNode entry = getEntry(key);
        if (entry == null) {
            entry = new ReferenceMapEntryNode(key, value);



            entriesByKey.put(key, entry);

            // entries.entrySet().add(entry);



            return this;
        }
        entry.setRaw(value);
        return this;
    }

    @Override
    public ReferenceMapNode remove(ReferenceNode key) {
        entriesByKey.remove(key);
        return this;
    }

    @Override
    public boolean containsKey(String key) {
        for (ReferenceNode keyNode : entriesByKey.keySet()) {
            if (keyNode instanceof ReferenceScalarNode scalar && key.equals(scalar.asString())) {
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
        for (ReferenceMapEntryNode entry : entriesByKey.values()) {
            ReferenceNode kNode = entry.getKey();
            // Check if the existing key's string value matches the requested key
            if (kNode instanceof ReferenceScalarNode scalar && key.equals(scalar.asString())) {
                entry.setRaw(value);
                return this;
            }
        }

        // Only if not found, create the new entry
        ReferenceNode keyNode = new ReferenceScalarNode(key, QuoteStyle.PLAIN);
        ReferenceMapEntryNode entry = new ReferenceMapEntryNode(keyNode, value);
        entriesByKey.put(entry.getKey(), entry);
        return this;
    }

    @Override
    public ReferenceMapNode remove(String key) {
        for (Map.Entry<ReferenceNode,ReferenceMapEntryNode> entry : entriesByKey.entrySet()) {
            if (entry.getKey() instanceof ReferenceScalarNode scalar) {
                if (scalar.asString().equals(key)) {
                    entriesByKey.remove(scalar);
                    return this;
                }
            }
        }
        return this;
    }

    @Override
    public ReferenceNode get(String key) {
        if (key == null) return null;

        for (Map.Entry<ReferenceNode,ReferenceMapEntryNode> entry : entriesByKey.entrySet()) {
            ReferenceMapEntryNode entryNode = entry.getValue();

            ReferenceNode kNode = entryNode.getKey();
            String entryKey = null;
            if (kNode instanceof ReferenceScalarNode scalar) {
                entryKey = scalar.asString();
            } else {
                entryKey = kNode.toString();
            }

            if (key.equals(entryKey)) {
                ReferenceNode val = entryNode.getValue();
                // return (val instanceof ReferenceAnchorNode a) ? a.getInnerNode() : val;
                return val;
            }
        }
        return null;
    }

    @Override
    public List<ReferenceMapEntryNode> getChildren() {
        return List.copyOf(entriesByKey.values());
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

    /// Returns Iterator instance
    @Override
    public Iterator<ReferenceMapEntryNode> iterator() {
        return entriesByKey.sequencedValues().iterator();
        // return new MapEntryIterator<JsonNode>(entriesByKey);
    }
}
