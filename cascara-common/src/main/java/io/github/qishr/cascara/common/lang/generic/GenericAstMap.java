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

package io.github.qishr.cascara.common.lang.generic;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.exception.UnimplementedMethodException;
import io.github.qishr.cascara.common.lang.ast.*;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;


public final class GenericAstMap extends GenericAstNode implements MapAstNode<AstNode, AstNode, GenericAstMapEntry> {

    // private List<GenericAstMapEntry> entries = new ArrayList<>();
    // private final LinkedHashMap<PlainNode,PlainNode> entries = new LinkedHashMap<>();
    private final LinkedHashMap<AstNode,GenericAstMapEntry> entriesByKey = new LinkedHashMap<>();

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
        if (key instanceof GenericAstNode node) {
            return getEntry(node) != null;
        } else if (key instanceof String string) {
            for (AstNode keyNode : entriesByKey.keySet()) {
                if (keyNode instanceof ScalarAstNode scalar && string.equals(scalar.asString())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<AstNode> keySet() {
        return entriesByKey.keySet();
    }

    @Override
    public AstNode get(Object key) {
        if (key instanceof String string) {
            for (Map.Entry<AstNode,GenericAstMapEntry> entry : entriesByKey.entrySet()) {
                GenericAstMapEntry entryNode = entry.getValue();
                Object keyObj = entryNode.getKey();
                String entryKey = null;
                if (keyObj instanceof ScalarAstNode scalar) {
                    entryKey = scalar.asString();
                } else {
                    entryKey = keyObj.toString();
                }

                if (string.equals(entryKey)) {
                    AstNode val = entryNode.getValue();
                    return val;
                }
            }
        }
        MapEntryAstNode<?,?> value = getEntry(key);
        return value == null ? null : value.getValue();
    }

    @Override
    public GenericAstMapEntry getEntry(Object key) {
        return entriesByKey.get(key);
    }

    @Override
    public GenericAstMapEntry getEntry(int i) {
        if (i < 0 || i > size()) throw new NoSuchElementException();
        return entriesByKey.sequencedValues().toArray(new GenericAstMapEntry[]{})[i];
    }

    @Override
    public List<GenericAstMapEntry> getEntries() {
        return List.copyOf(entriesByKey.values());
    }

    @Override
    public Set<GenericAstMapEntry> entrySet() {
        return new LinkedHashSet<>(entriesByKey.values());
    }

    @Override
    public List<AstNode> values() {
        throw new UnimplementedMethodException();
    }

    @Override
    public GenericAstMap put(AstNode key, AstNode value) {
        GenericAstMapEntry entry = getEntry(key);
        if (entry == null) {
            entry = new GenericAstMapEntry(key, value);
            entriesByKey.put(key, entry);
            return this;
        }
        entry.setRaw(value);
        return this;
    }

    @Override
    public GenericAstMap remove(AstNode key) {
        entriesByKey.remove(key);
        return this;
    }

    @Override
    public GenericAstMap put(String key, String value) {
        GenericAstScalar scalarValue = new GenericAstScalar(value);
        put(key, scalarValue);
        return this;
    }

    public GenericAstMap put(String key, Number value) {
        GenericAstScalar scalarValue = new GenericAstScalar(value);
        put(key, scalarValue);
        return this;
    }

    @Override
    public GenericAstMap put(String key, AstNode value) {
        for (GenericAstMapEntry entry : entriesByKey.values()) {
            AstNode kNode = entry.getKey();
            // Check if the existing key's string value matches the requested key
            if (kNode instanceof GenericAstScalar scalar && key.equals(scalar.asString())) {
                entry.setRaw(value);
                return this;
            }
        }

        // Only if not found, create the new entry
        AstNode keyNode = new GenericAstScalar(key, QuoteStyle.PLAIN);
        GenericAstMapEntry entry = new GenericAstMapEntry(keyNode, value);
        entriesByKey.put(entry.getKey(), entry);
        return this;
    }

    @Override
    public GenericAstMap remove(String key) {
        for (Map.Entry<AstNode,GenericAstMapEntry> entry : entriesByKey.entrySet()) {
            if (entry.getKey() instanceof GenericAstScalar scalar) {
                if (scalar.asString().equals(key)) {
                    entriesByKey.remove(scalar);
                    return this;
                }
            }
        }
        return this;
    }

    @Override
    public List<GenericAstMapEntry> getChildren() {
        return List.copyOf(entriesByKey.values());
    }

    @Override
    public List<CommentAstNode> getComments() {
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }

    //
    // Convenience Getters
    //

    @Override
    @Nullable
    public GenericAstMap getMap(Object key) {
        if (get(key) instanceof GenericAstMap map) {
            return map;
        }
        return null;
    }

    @Override
    @Nullable
    public SequenceAstNode<AstNode> getSequence(Object key) {
        if (get(key) instanceof GenericAstSequence seq) {
            return seq;
        }
        return null;
    }

    @Override
    @Nullable
    public ScalarAstNode<AstNode> getScalar(Object key) {
        if (get(key) instanceof GenericAstScalar scalar) {
            return scalar;
        }
        return null;
    }

    //
    //
    //

    /// Returns Iterator instance
    @Override
    public Iterator<GenericAstMapEntry> iterator() {
        return entriesByKey.sequencedValues().iterator();
    }
}
