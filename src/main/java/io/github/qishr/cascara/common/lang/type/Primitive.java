package io.github.qishr.cascara.common.lang.type;

import java.util.Objects;

import io.github.qishr.cascara.common.lang.QuoteStyle;

public class Primitive {
    protected final Object rawInput;
    private final QuoteStyle originalQuotedStyle;
    private final boolean isAlreadyNative;
    private PrimitiveDelegate delegate;
    protected QuoteStyle specifiedQuoteStyle;

    public static Primitive of(Object nativeInstance) {
        Primitive primitive = new Primitive(nativeInstance, null, true);
        return primitive;
    }

    /// Parses unescaped text and infers its type.
    public static Primitive fromString(String unescapedContent, QuoteStyle quoteStyle) {
        return new Primitive(unescapedContent, quoteStyle, false);
    }

    private Primitive(Object rawInput, QuoteStyle quoteStyle, boolean isNative) {
        this.rawInput = rawInput;
        this.originalQuotedStyle = quoteStyle;
        this.isAlreadyNative = isNative;
    }

    public Primitive setDelegate(PrimitiveDelegate delegate) {
        this.delegate = delegate;
        return this;
    }

    public Object unwrap() { return nativeValue(); }

    public QuoteStyle getQuoteStyle() {
        if (specifiedQuoteStyle != null) {
            return specifiedQuoteStyle;
        }

        if (originalQuotedStyle == null) {
            // TODO: Do we want to cache this?
            return inferQuoteStyle(rawInput);
        }
        return originalQuotedStyle;
    }

    public Primitive setQuoteStyle(QuoteStyle style) {
        this.specifiedQuoteStyle = style;
        return this;
    }

    public String asString() {
        Object nativeValue = nativeValue();
        return nativeValue == null ? null : String.valueOf(nativeValue);
    }

    public double asDouble(double defaultValue) {
        if (nativeValue() instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(asString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int asInteger(int defaultValue) {
        Object nativeValue = nativeValue();
        if (nativeValue == null) return defaultValue;
        if (nativeValue instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(nativeValue.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean asBoolean(boolean defaultValue) {
        Object nativeValue = nativeValue();
        if (nativeValue instanceof Boolean bool) return bool;
        if (nativeValue instanceof String str) {
            return Boolean.parseBoolean(str.trim());
        }
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Primitive honesty = (Primitive) o;
        return Objects.equals(nativeValue(), honesty.nativeValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(nativeValue());
    }

    @Override
    public String toString() {
        return asString();
    }

    //
    // Delegates
    //

    private QuoteStyle inferQuoteStyle(Object value) {
        if (delegate != null) {
            return delegate.inferQuoteStyle(value);
        }
        return QuoteStyle.PLAIN;
    }

    /// Intercept and resolve dialect-specific keywords (e.g., booleans, nulls).
    private Object coerceLiteralValue(String text) {
        if (delegate != null) {
            return delegate.coerceLiteralValue(text);
        }
        return null;
    }

    /// Default baseline unescaping hook. Can be overridden if a language has specific string escape sequences.
    private String unescapeQuotedString(String text, QuoteStyle style) {
        if (delegate != null) {
            return delegate.unescapeQuotedString(text, style);
        }
        return text; // Fallback if the token stream already handles baseline unescaping
    }

    //
    //
    //

    private Object nativeValue() {
        if (isAlreadyNative) {
            // eg Integer, Boolean
            return rawInput;
        } else {
            // A String
            // TODO: Cache these values for returning next time
            if (originalQuotedStyle != QuoteStyle.PLAIN) {
                // Unescape contextually based on the quoting rules of the dialect
                return unescapeQuotedString(rawInput == null ? "" : rawInput.toString(), originalQuotedStyle);
            } else {
                return coerce(rawInput);
            }
        }
    }

    /// Iterates through core conversions. Can be overridden for language-specific nuances.
    private Object coerce(Object input) {
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
}