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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.lang.ast.*;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;


public final class PlainMapNode extends PlainNode implements MapAstNode<PlainNode, PlainNode, PlainMapEntryNode> {

    // private List<PlainMapEntryNode> entries = new ArrayList<>();
    // private final LinkedHashMap<PlainNode,PlainNode> entries = new LinkedHashMap<>();
    private final LinkedHashMap<PlainNode,PlainMapEntryNode> entriesByKey = new LinkedHashMap<>();

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
    public boolean containsKey(Object key) {
        if (key instanceof PlainNode node) {
            return getEntry(node) != null;
        } else if (key instanceof String string) {
            for (PlainNode keyNode : entriesByKey.keySet()) {
                if (keyNode instanceof PlainScalarNode scalar && string.equals(scalar.asString())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<PlainNode> keySet() {
        return entriesByKey.keySet();
    }

    @Override
    public PlainNode get(Object key) {
        if (key instanceof String string) {
            for (Map.Entry<PlainNode,PlainMapEntryNode> entry : entriesByKey.entrySet()) {
                PlainMapEntryNode entryNode = entry.getValue();

                PlainNode kNode = entryNode.getKey();
                String entryKey = null;
                if (kNode instanceof PlainScalarNode scalar) {
                    entryKey = scalar.asString();
                } else {
                    entryKey = kNode.toString();
                }

                if (string.equals(entryKey)) {
                    PlainNode val = entryNode.getValue();
                    return val;
                }
            }
        }
        PlainMapEntryNode value = getEntry(key);
        return value == null ? null : value.getValue();
    }

    @Override
    public PlainMapEntryNode getEntry(Object key) {
        return entriesByKey.get(key);
    }

    @Override
    public PlainMapEntryNode getEntry(int i) {
        if (i < 0 || i > size()) throw new NoSuchElementException();
        return entriesByKey.sequencedValues().toArray(new PlainMapEntryNode[]{})[i];
    }

    @Override
    public List<PlainMapEntryNode> getEntries() {
        return List.copyOf(entriesByKey.values());
    }

    @Override
    public Set<PlainMapEntryNode> entrySet() {
        return new LinkedHashSet<>(entriesByKey.values());
    }

    @Override
    public List<PlainNode> values() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'values'");
    }

    @Override
    public PlainMapNode put(PlainNode key, PlainNode value) {



        PlainMapEntryNode entry = getEntry(key);
        if (entry == null) {
            entry = new PlainMapEntryNode(key, value);



            entriesByKey.put(key, entry);

            // entries.entrySet().add(entry);



            return this;
        }
        entry.setRaw(value);
        return this;
    }

    @Override
    public PlainMapNode remove(PlainNode key) {
        entriesByKey.remove(key);
        return this;
    }

    @Override
    public PlainMapNode put(String key, String value) {
        PlainScalarNode scalarValue = new PlainScalarNode(value);
        put(key, scalarValue);
        return this;
    }

    @Override
    public PlainMapNode put(String key, PlainNode value) {
        for (PlainMapEntryNode entry : entriesByKey.values()) {
            PlainNode kNode = entry.getKey();
            // Check if the existing key's string value matches the requested key
            if (kNode instanceof PlainScalarNode scalar && key.equals(scalar.asString())) {
                entry.setRaw(value);
                return this;
            }
        }

        // Only if not found, create the new entry
        PlainNode keyNode = new PlainScalarNode(key, QuoteStyle.PLAIN);
        PlainMapEntryNode entry = new PlainMapEntryNode(keyNode, value);
        entriesByKey.put(entry.getKey(), entry);
        return this;
    }

    @Override
    public PlainMapNode remove(String key) {
        for (Map.Entry<PlainNode,PlainMapEntryNode> entry : entriesByKey.entrySet()) {
            if (entry.getKey() instanceof PlainScalarNode scalar) {
                if (scalar.asString().equals(key)) {
                    entriesByKey.remove(scalar);
                    return this;
                }
            }
        }
        return this;
    }

    // @Override
    // public PlainNode get(String key) {
    //     if (key == null) return null;

    //     for (Map.Entry<PlainNode,PlainMapEntryNode> entry : entriesByKey.entrySet()) {
    //         PlainMapEntryNode entryNode = entry.getValue();

    //         PlainNode kNode = entryNode.getKey();
    //         String entryKey = null;
    //         if (kNode instanceof PlainScalarNode scalar) {
    //             entryKey = scalar.asString();
    //         } else {
    //             entryKey = kNode.toString();
    //         }

    //         if (key.equals(entryKey)) {
    //             PlainNode val = entryNode.getValue();
    //             // return (val instanceof ReferenceAnchorNode a) ? a.getInnerNode() : val;
    //             return val;
    //         }
    //     }
    //     return null;
    // }

    @Override
    public List<PlainMapEntryNode> getChildren() {
        return List.copyOf(entriesByKey.values());
    }

    @Override
    public List<CommentAstNode> getComments() {
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }

    @Override
    public PlainMapNode getMap(Object key) {
        throw new UnsupportedOperationException("Unimplemented method 'getMap'");
    }

    @Override
    public PlainSequenceNode getSequence(Object key) {
        throw new UnsupportedOperationException("Unimplemented method 'getSequence'");
    }

    @Override
    @Nullable
    public PlainScalarNode getScalar(Object key) {
        if (get(key) instanceof PlainScalarNode scalar) {
            return scalar;
        }
        return null;
    }

    /// Returns Iterator instance
    @Override
    public Iterator<PlainMapEntryNode> iterator() {
        return entriesByKey.sequencedValues().iterator();
        // return new MapEntryIterator<JsonNode>(entriesByKey);
    }
}
