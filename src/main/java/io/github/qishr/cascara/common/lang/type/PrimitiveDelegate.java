package io.github.qishr.cascara.common.lang.type;

import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public interface PrimitiveDelegate {
    QuoteStyle inferQuoteStyle(Object value);
    Object coerceLiteralValue(String text);
    String unescapeQuotedString(String text, QuoteStyle style);
}
