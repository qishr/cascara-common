package io.github.qishr.cascara.common.util;

public class Property {
    Kind kind = Kind.STRING;
    String key;
    String value = null;

    public Property(String k) {
        key = k;
    }

    public Property(String k, String v) {
        key = k;
        value = v;
    }

    public String getKey() {
        return key;
    }

    public void setName(String k) {
        key = k;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getString() {
        return value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String v) {
        value = v;
        kind = Kind.STRING;
    }

    public void setValue(boolean v) {
        value = v ? "true" : "false";
        kind = Kind.BOOLEAN;
    }

    public void setValue(int v) {
        value = Long.toString(v);
        kind = Kind.NUMBER;
    }

    public void setValue(double v) {
        value = Double.toString(v);
        kind = Kind.NUMBER;
    }

    public double getDouble() {
        return getDouble(-1);
    }

    public double getDouble(double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getInt() {
        return getInt(-1);
    }

    public long getLong() {
        return getLong(-1);
    }

    public int getInt(int defaultValue) {
        return (int) getLong(defaultValue);
    }

    public long getLong(int defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
        }
        try {
            return Double.valueOf(value).longValue();
        } catch (NumberFormatException e) {
        }
        return defaultValue;
    }

    public boolean getBoolean() {
        return getBoolean(false);
    }

    public boolean getBoolean(boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return (value.equalsIgnoreCase("true") ||
             value.equalsIgnoreCase("yes"));
    }

    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    public enum Kind {
        STRING,
        NUMBER,
        BOOLEAN
    }
}
