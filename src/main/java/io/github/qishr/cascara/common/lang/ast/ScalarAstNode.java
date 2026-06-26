package io.github.qishr.cascara.common.lang.ast;

import io.github.qishr.cascara.common.lang.annotation.Nullable;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public interface ScalarAstNode<T extends AstNode> extends AstNode {
    /// Returns the [QuoteStyle] used by a node: PLAIN, SINGLE, DOUBLE, LITERAL, or FOLDED.
    QuoteStyle getQuoteStyle();

    /// Sets the [QuoteStyle] used by a node
    void setQuoteStyle(QuoteStyle style);

    /// Returns the exact, unparsed text block directly from the file buffer.
    @Nullable
    String getRaw();

    /// Returns the Java-native representation of the scalar (e.g., Integer, Boolean, String).
    @Nullable
    Object getPrimitive();

    /// Updates the logical native primitive value of this node.
    ///
    /// This method invalidates any pre-existing raw string cache derived from
    /// a file buffer, marking the node as dirty so the emitter can contextually
    /// re-serialize the new value on the next text export pass.
    ScalarAstNode<T> setPrimitive(Object value);

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