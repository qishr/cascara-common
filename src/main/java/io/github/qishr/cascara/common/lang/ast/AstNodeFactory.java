package io.github.qishr.cascara.common.lang.ast;

import io.github.qishr.cascara.common.lang.type.Primitive;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public interface AstNodeFactory<
    N extends AstNode,
    S extends ScalarAstNode<N>,
    L extends SequenceAstNode<N>,
    M extends MapAstNode<N,E>,
    E extends MapEntryAstNode<N>
> {
    S createScalarNode(Object primitiveValue);
    S createScalarNode(Object primitiveValue, QuoteStyle quoteStyle);
    S createScalarNode(Primitive primitive);
    N createScalarKeyNode(Object key);
    L createSequenceNode();
    M createMapNode();
}
