package io.github.qishr.cascara.yaml.ast;

import java.util.Collections;
import java.util.List;

// Assuming YamlNode is in the same package and has the tree structure methods
public class YamlMappingEntry extends YamlNode {

    // The key and value are stored as children in the base YamlNode:
    // children.get(0) is the key, children.get(1) is the value.

    /**
     * **NEW:** Constructor for creating a YamlMappingEntry during parsing or manual AST building.
     * It correctly adds the key and value as children.
     * * @param key The YamlNode representing the map key.
     * @param value The YamlNode representing the map value.
     */
    public YamlMappingEntry(YamlNode key, YamlNode value) {
        // Clear any existing children (though this constructor should only be called once)
        this.children.clear();

        // Key is always the first child
        addChild(key);

        // Value is always the second child
        addChild(value);
    }

    // **NEW:** Default constructor for parser flexibility (key and value added later)
    public YamlMappingEntry() {
        // Empty
    }

    /**
     * **NEW:** Updates the value node of this map entry.
     * Used by YamlMap.setEntryValue() when an existing key is found.
     * * @param newValue The new YamlNode to replace the current value.
     */
    public void setValue(YamlNode newValue) {
        // Ensure the list is large enough to contain the value (index 1).
        while (children.size() < 2) {
            // Add null or an empty placeholder if only the key existed.
            // In practice, this should only be called if a key already exists (children.size() >= 1).
            children.add(null);
        }

        // 1. Remove the old value node from the AST structure (index 1)
        if (children.size() > 1 && children.get(1) != null) {
             children.get(1).setParent(null);
        }

        // 2. Insert the new value node at the correct position (index 1)
        children.set(1, newValue);

        // 3. Set the new value's parent
        if (newValue != null) {
            newValue.setParent(this);
        }
    }

    // --- Existing Read Methods ---

    /**
     * Retrieves the key node of this map entry.
     * @return The YamlNode representing the key (typically a YamlScalar).
     */
    public YamlNode getKey() {
        if (children.isEmpty()) {
            return null;
        }
        return children.get(0);
    }

    public String getKeyAsString() {
        YamlNode keyNode = getKey();
        if (keyNode instanceof YamlScalar scalar) {
            return scalar.asString();
        }
        return null;
    }

    /**
     * Retrieves the value node of this map entry.
     * @return The YamlNode representing the value (can be any type: Scalar, Map, Sequence).
     */
    public YamlNode getValue() {
        if (children.size() < 2) {
            // A key with no value (e.g., in a block map entry "key:") is implicitly null/empty
            return null;
        }
        return children.get(1);
    }

    public String getValueAsString() {
        YamlNode valueNode = getValue();
        if (valueNode instanceof YamlScalar scalar) {
            return scalar.asString();
        }
        return null;
    }

    public int getValueAsInteger() {
        YamlNode valueNode = getValue();
        if (valueNode instanceof YamlScalar scalar) {
            // NOTE: This assumes YamlScalar has an asInteger() method
            return scalar.asInteger();
        }
        return -1;
    }

    public boolean getValueAsBoolean() {
        YamlNode valueNode = getValue();
        if (valueNode instanceof YamlScalar scalar) {
            // NOTE: This assumes YamlScalar has an asBoolean() method
            return scalar.asBoolean();
        }
        return false;
    }

    public List<YamlNode> getValueAsSequence() {
        YamlNode valueNode = getValue();
        if (valueNode instanceof YamlSequence sequence) {
            return sequence.getChildren();
        }
        return Collections.emptyList();
    }
}