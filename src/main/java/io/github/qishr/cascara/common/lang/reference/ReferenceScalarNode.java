package io.github.qishr.cascara.common.lang.reference;

import io.github.qishr.cascara.common.lang.QuoteStyle;
import io.github.qishr.cascara.common.lang.annotation.Nullable;
import io.github.qishr.cascara.common.lang.ast.*;
import java.util.Collections;
import java.util.List;

public final class ReferenceScalarNode extends ReferenceNode implements ScalarAstNode<ReferenceNode> {

    private Object value;
    private QuoteStyle quoteStyle = QuoteStyle.PLAIN;

    public ReferenceScalarNode(Object value) {
        this.value = value;
    }

    @Override
    public void setQuoteStyle(QuoteStyle style) {
        this.quoteStyle = style;
    }

    @Override
    @Nullable
    public String getRaw() {
        return value == null ? null : value.toString();
    }

    @Override
    public String asString() {
        return value == null ? "" : value.toString();
    }

    @Override
    public int asInteger() {
        return asInteger(0);
    }

    @Override
    public int asInteger(int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(asString()); } catch (Exception e) { return defaultValue; }
    }

    @Override
    public double asDouble() {
        return asDouble(0);
    }

    @Override
    public double asDouble(double defaultValue) {
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(asString()); } catch (Exception e) { return defaultValue; }
    }

    @Override
    public boolean asBoolean() {
        return asBoolean(false);
    }

    @Override
    public boolean asBoolean(boolean defaultValue) {
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(asString());
    }

    @Nullable
    @Override
    public Object getPrimitive() {
        return value;
    }

    @Override
    public ReferenceScalarNode setPrimitive(Object value) {
        this.value = value;
        return this;
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
