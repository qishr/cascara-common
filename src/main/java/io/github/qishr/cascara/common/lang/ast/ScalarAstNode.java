package io.github.qishr.cascara.common.lang.ast;

import io.github.qishr.cascara.common.lang.QuoteStyle;

public interface ScalarAstNode<T extends AstNode> extends AstNode {
    void setQuoteStyle(QuoteStyle style);
    QuoteStyle getQuoteStyle();
    void setValue(String value);
    String getRawValue();
    String getString();
    int getInteger();
    int getInteger(int defaultValue);
    double getDouble();
    double getDouble(double defaultValue);
    boolean getBoolean();
    boolean getBoolean(boolean defaultValue);

    /// Returns the Java-native representation of the scalar (e.g., Integer, Boolean, String).
    Object getPrimitiveValue();
    void setPrimitiveValue(Object value);
}