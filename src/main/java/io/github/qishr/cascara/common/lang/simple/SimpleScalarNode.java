package io.github.qishr.cascara.common.lang.simple;

import io.github.qishr.cascara.common.lang.QuoteStyle;
import io.github.qishr.cascara.common.lang.ast.*;
import java.util.Collections;
import java.util.List;

public final class SimpleScalarNode extends SimpleNode implements ScalarAstNode<SimpleNode> {

    private Object value;
    private QuoteStyle quoteStyle = QuoteStyle.PLAIN;

    public SimpleScalarNode(Object value) {
        this.value = value;
    }

    @Override
    public void setQuoteStyle(QuoteStyle style) {
        this.quoteStyle = style;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getRawValue() {
        return value == null ? "null" : value.toString();
    }

    @Override
    public String getString() {
        return value == null ? "" : value.toString();
    }

    @Override
    public int getInteger() {
        return getInteger(0);
    }

    @Override
    public int getInteger(int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(getString()); } catch (Exception e) { return defaultValue; }
    }

    @Override
    public double getDouble() {
        return getDouble(0);
    }

    @Override
    public double getDouble(double defaultValue) {
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(getString()); } catch (Exception e) { return defaultValue; }
    }

    @Override
    public boolean getBoolean() {
        return getBoolean(false);
    }

    @Override
    public boolean getBoolean(boolean defaultValue) {
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(getString());
    }

    @Override
    public Object getPrimitiveValue() {
        return value;
    }

    @Override
    public void setPrimitiveValue(Object value) {
        this.value = value;
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public List<CommentAstNode> getComments() {
        return Collections.emptyList();
    }

    @Override
    public QuoteStyle getQuoteStyle() {
        return quoteStyle;
    }
}
