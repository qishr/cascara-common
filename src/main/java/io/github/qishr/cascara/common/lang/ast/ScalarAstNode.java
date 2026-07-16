package io.github.qishr.cascara.common.lang.ast;

import io.github.qishr.cascara.common.lang.annotation.Nullable;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public interface ScalarAstNode<T extends AstNode> extends AstNode {
    /// Returns the [QuoteStyle] used by a node: PLAIN, SINGLE, DOUBLE, LITERAL, or FOLDED.
    QuoteStyle getQuoteStyle();

    /// Sets the [QuoteStyle] used by a node
    ScalarAstNode<T> setQuoteStyle(QuoteStyle style);

    /// Returns the exact, unparsed text block directly from the file buffer.
    @Nullable
    String getLexeme();

    /// Returns the Java-native representation of the scalar (e.g., Integer, Boolean, String).
    @Nullable
    Object getPrimitive();

    @Nullable
    String getContent();

    /// Returns the string form or the primitive.
    /// If the primitive is `null`, an empty string is returned.
    String asString();

    int asInteger();
    int asInteger(int defaultValue);
    double asDouble();
    double asDouble(double defaultValue);

    /// Returns the boolean value of the scalar, if there is one.
    boolean asBoolean();

    /// Returns the boolean value of the scalar, if there is one, otherwise the specified default is returned.
    boolean asBoolean(boolean defaultValue);
}