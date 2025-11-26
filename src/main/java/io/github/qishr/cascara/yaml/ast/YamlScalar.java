package io.github.qishr.cascara.yaml.ast;

import java.util.Objects;

public class YamlScalar extends YamlNode {

    /**
     * Defines the various styles a scalar node can take in a YAML file.
     */
    public enum ScalarStyle {
        PLAIN,
        SINGLE_QUOTED,
        DOUBLE_QUOTED,
        LITERAL,
        FOLDED
    }

    /**
     * The actual, type-converted Java object.
     * REMOVED 'final' to allow modification for document creation/editing.
     */
    private Object value;

    /**
     * The style of the scalar as it appeared in the source file.
     * We keep 'final' for style as it typically defines the original form.
     */
    private final ScalarStyle style;

    // --- Constructors ---

    /**
     * Constructs a YamlScalar node.
     * @param value The type-converted value.
     * @param style The style used in the YAML source.
     */
    public YamlScalar(Object value, ScalarStyle style) {
        this.value = value; // Direct assignment is safe now that 'value' is not final
        this.style = style;
    }

    /**
     * NEW: Default constructor for creating an empty node (will default to PLAIN style).
     */
    public YamlScalar() {
        this(null, ScalarStyle.PLAIN);
    }

    // --- Accessor/Mutator Methods ---

    /**
     * Retrieves the type-converted Java value.
     * @return The parsed object.
     */
    public Object getValue() {
        return value;
    }

    /**
     * NEW: Sets the type-converted Java value.
     * Used for creating or modifying the AST programmatically.
     * @param value The new value (e.g., String, Integer, Boolean, or null).
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Retrieves the scalar style from the source file.
     * @return The style enum.
     */
    public ScalarStyle getStyle() {
        return style;
    }

    // --- Convenience Accessor Methods (Unchanged) ---

    public String asString() {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public Integer asInteger() {
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return null;
    }

    public Boolean asBoolean() {
        if (value instanceof Boolean b) return b;
        return null;
    }

    @Override
    public String toString() {
        String val = Objects.toString(value, "null");
        // Updated to use the value's actual class name safely, especially for null handling.
        String valueClassName = (value != null) ? value.getClass().getSimpleName() : "Null";
        return "YamlScalar(Value=" + val + ", Type=" + valueClassName + ", Style=" + style + ")";
    }
}