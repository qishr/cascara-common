package io.github.qishr.cascara.common.lang.type;

import java.util.Objects;

import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public class Primitive {
    private PrimitiveDelegate delegate;
    protected final Object rawInput;
    private Object nativeValueCache;
    private boolean nativeValueCacheIsCached;
    private final boolean isAlreadyNative;
    private final QuoteStyle originalQuotedStyle;
    protected QuoteStyle specifiedQuoteStyle;

    private boolean stringIsCached;
    private String stringCache;

    public static Primitive of(Object nativeInstance) {
        return new Primitive(nativeInstance, null, true);
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
            // Do we want to cache this?
            // If we really want to cache it, then invalidate the cache when the delegate gets set
            return inferQuoteStyle(rawInput);
        }
        return originalQuotedStyle;
    }

    public Primitive setQuoteStyle(QuoteStyle style) {
        this.specifiedQuoteStyle = style;
        return this;
    }

    public String asString() {
        if (!stringIsCached) {
            Object nativeValue = nativeValue();
            stringCache = nativeValue == null ? null : String.valueOf(nativeValue);
            stringIsCached = true;
        }
        return stringCache;
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
        if (!nativeValueCacheIsCached) {
            if (isAlreadyNative) {
                // eg Integer, Boolean
                nativeValueCache = rawInput;
            } else {
                // A String
                if (originalQuotedStyle != QuoteStyle.PLAIN) {
                    // Unescape contextually based on the quoting rules of the dialect
                    nativeValueCache = unescapeQuotedString(rawInput == null ? "" : rawInput.toString(), originalQuotedStyle);
                } else {
                    nativeValueCache = coerce(rawInput);
                }
            }
            nativeValueCacheIsCached = true;
        }
        return nativeValueCache;
    }

    /// Iterates through core conversions. Can be overridden for language-specific nuances.
    private Object coerce(Object input) {
        if (input == null) return null;
        if (!(input instanceof String str)) return input;

        // Give the concrete subclass a chance to handle language keywords (like YAML's yes/no/on/off)
        Object customCoercion = coerceLiteralValue(str);
        if (customCoercion != null) return customCoercion;

        // Fall back to baseline universal primitives
        // String trimmed = str.trim();

        // Universal numbers
        if (isLikelyNumeric(str)) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) { /* move on */ }

            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) { /* move on */ }
        }

        return str;
    }

    private static boolean isLikelyNumeric(String str) {
        if (str == null || str.isEmpty()) return false;

        // Explicitly treat unquoted Infinity variations as numeric names
        if (str.endsWith("Infinity") || "NaN".equals(str)) {
            return true;
        }

        char first = str.charAt(0);
        // Quick check for standard start characters
        if (!Character.isDigit(first) && first != '-' && first != '+' && first != '.') {
            return false;
        }
        // Optional: scan to ensure there's at least one digit
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) return true;
        }
        return false;
    }
}