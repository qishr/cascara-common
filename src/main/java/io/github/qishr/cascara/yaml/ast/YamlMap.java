package io.github.qishr.cascara.yaml.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YamlMap extends YamlNode {

    private final List<YamlMappingEntry> entries = new ArrayList<>();

    /**
     * Adds a complete key-value entry (encapsulated by YamlMappingEntry) to the map.
     * This method correctly updates the AST hierarchy by calling addChild().
     * @param entry The YamlMappingEntry containing the key and value nodes.
     */
    public void addEntry(YamlMappingEntry entry) {
        // 1. MANDATORY: Add to the specialized list for getEntries()
        this.entries.add(entry);

        // 2. MANDATORY: Add to the generic children list for getChildren()
        super.addChild(entry);
    }

    /**
     * Retrieves the list of all mapping entries in their original order.
     * @return A read-only List of YamlMappingEntry objects.
     */
    @SuppressWarnings("unchecked")
    public List<YamlMappingEntry> getEntries() {
        return Collections.unmodifiableList(this.entries);
    }

    /**
     * Convenience method to find a value by its literal String key.
     * NOTE: This requires the key node to be a YamlScalar with a String value.
     * @param keyString The string representation of the key to look up.
     * @return The YamlNode value associated with the key, or null if not found.
     */
    public YamlNode getValueByKey(String keyString) {
        for (YamlMappingEntry entry : getEntries()) {
            YamlNode keyNode = entry.getKey();

            if (keyNode instanceof YamlScalar scalar) {
                Object scalarValue = scalar.getValue();
                if (scalarValue instanceof String && keyString.equals(scalarValue)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // READ (Getter) Methods
    // -------------------------------------------------------------------------

    /**
     * Retrieves the String scalar value associated with a key.
     * @param keyString The map key.
     * @return The String value, or null if the key is not found or the value is not a scalar.
     */
    public String getString(String keyString) {
        YamlNode node = getValueByKey(keyString);
        if (node instanceof YamlScalar scalar) {
            return scalar.asString();
        }
        return null;
    }

    /**
     * Retrieves the YamlMap value associated with a key.
     * @param keyString The map key.
     * @return The nested YamlMap, or null if the key is not found or the value is not a map.
     */
    public YamlMap getMap(String keyString) {
        YamlNode node = getValueByKey(keyString);
        if (node instanceof YamlMap map) {
            return map;
        }
        return null;
    }

    /**
     * Retrieves the items (children) of the YamlSequence value associated with a key.
     * @param keyString The map key.
     * @return A List of YamlNode items, or an empty list if the key is not found or the value is not a sequence.
     */
    public List<YamlNode> getSequence(String keyString) {
        YamlNode node = getValueByKey(keyString);
        if (node instanceof YamlSequence sequence) {
            return sequence.getChildren(); // <-- **FIXED: Returns the list of children**
        }
        return Collections.emptyList(); // <-- Returns an empty list instead of null
    }

    // -------------------------------------------------------------------------
    // WRITE (Setter/Creator) Methods
    // -------------------------------------------------------------------------

    /**
     * Helper to find an existing entry or create a new one, then set its value.
     * @param keyString The map key (always treated as a plain scalar).
     * @param newValue The new YamlNode to be the value.
     */
    private void setEntryValue(String keyString, YamlNode newValue) {
        // 1. Try to find existing entry
        for (YamlMappingEntry entry : this.entries) {
            YamlNode keyNode = entry.getKey();
            if (keyNode instanceof YamlScalar scalar && keyString.equals(scalar.getValue())) {
                entry.setValue(newValue); // Assume YamlMappingEntry has a setValue method
                return;
            }
        }

        // 2. If not found, create a new entry and add it
        // NOTE: We assume a constructor for YamlScalar exists that accepts a String for a plain scalar.
        YamlScalar newKey = new YamlScalar(keyString, YamlScalar.ScalarStyle.PLAIN);
        YamlMappingEntry newEntry = new YamlMappingEntry(newKey, newValue); // Assume YamlMappingEntry constructor exists

        this.addEntry(newEntry);
    }

    /**
     * Sets or creates a key-value entry with a String scalar value.
     * @param keyString The map key.
     * @param value The String value.
     * @return The updated YamlMap instance for fluent chaining.
     */
    public YamlMap setString(String keyString, String value) {
        // We assume a constructor for YamlScalar exists that accepts a String for its value.
        YamlScalar scalarValue = new YamlScalar(value, YamlScalar.ScalarStyle.PLAIN);
        setEntryValue(keyString, scalarValue);
        return this;
    }

    /**
     * Sets or creates a key-value entry with a nested YamlMap value.
     * @param keyString The map key.
     * @param mapValue The YamlMap instance.
     * @return The updated YamlMap instance for fluent chaining.
     */
    public YamlMap setMap(String keyString, YamlMap mapValue) {
        setEntryValue(keyString, mapValue);
        return this;
    }

    /**
     * Sets or creates a key-value entry with a YamlSequence value.
     * @param keyString The map key.
     * @param sequenceValue The YamlSequence instance.
     * @return The updated YamlMap instance for fluent chaining.
     */
    public YamlMap setSequence(String keyString, YamlSequence sequenceValue) {
        setEntryValue(keyString, sequenceValue);
        return this;
    }
}