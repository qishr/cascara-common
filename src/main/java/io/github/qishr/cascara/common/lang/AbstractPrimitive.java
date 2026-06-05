package io.github.qishr.cascara.common.lang;

public abstract class AbstractPrimitive {
    protected final Object nativeValue;
    protected QuoteStyle quoteStyle;

    protected AbstractPrimitive(Object primitiveValue) {
        nativeValue = primitiveValue;
        quoteStyle = inferQuoteStyle(primitiveValue);
    }

    protected AbstractPrimitive(Object input, QuoteStyle quoteStyle, boolean isNative) {
        this.quoteStyle = quoteStyle;

        if (isNative) {
            nativeValue = input;
        } else {
            if (quoteStyle != QuoteStyle.PLAIN) {
                // Unescape contextually based on the quoting rules of the dialect
                String unescaped = unescapeQuotedString(input == null ? "" : input.toString(), quoteStyle);
                this.nativeValue = unescaped;
            } else {
                this.nativeValue = coerce(input);
            }
        }
    }

    protected abstract QuoteStyle inferQuoteStyle(Object value);

    /// Default baseline unescaping hook. Can be overridden if a language has specific string escape sequences.
    protected String unescapeQuotedString(String text, QuoteStyle style) {
        return text; // Fallback if the token stream already handles baseline unescaping
    }

    /// Iterates through core conversions. Can be overridden for language-specific nuances.
    protected Object coerce(Object input) {
        if (input == null) return null;
        if (!(input instanceof String str)) return input;

        // Give the concrete subclass a chance to handle language keywords (like YAML's yes/no/on/off)
        Object customCoercion = coerceLiteralValue(str);
        if (customCoercion != null) return customCoercion;

        // Fall back to baseline universal primitives
        String trimmed = str.trim();

        // Universal numbers
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) { /* move on */ }

        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) { /* move on */ }

        return str;
    }

    /// Abstract hook to intercept and resolve dialect-specific keywords (e.g., booleans, nulls).
    protected abstract Object coerceLiteralValue(String text);

    public Object unwrap() { return nativeValue; }

    public QuoteStyle getQuoteStyle() {
        return quoteStyle;
    }

    public void setQuoteStyle(QuoteStyle style) {
        this.quoteStyle = style;
    }

    public String asString() {
        return nativeValue == null ? null : String.valueOf(nativeValue);
    }

    public double asDouble(double defaultValue) {
        if (nativeValue instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(asString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int asInteger(int defaultValue) {
        if (nativeValue == null) return defaultValue;
        if (nativeValue instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(nativeValue.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean asBoolean(boolean defaultValue) {
        if (nativeValue instanceof Boolean bool) return bool;
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractPrimitive honesty = (AbstractPrimitive) o;
        return java.util.Objects.equals(nativeValue, honesty.nativeValue);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(nativeValue);
    }

    @Override
    public String toString() {
        return asString();
    }
}