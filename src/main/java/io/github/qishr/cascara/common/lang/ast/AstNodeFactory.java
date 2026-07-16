package io.github.qishr.cascara.common.lang.ast;

import io.github.qishr.cascara.common.lang.util.LanguageOptions;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public interface AstNodeFactory<
    N extends AstNode,
    S extends ScalarAstNode<N>,
    L extends SequenceAstNode<N>,
    M extends MapAstNode<K,N,E>,
    E extends MapEntryAstNode<K,N>,
    K
> {
    S createScalarNode(Object jvmValue);
    S createScalarNode(Object jvmValue, QuoteStyle quoteStyle);
    S createScalarNode(Object jvmValue, QuoteStyle quoteStyle, LanguageOptions<?> options);
    K createKey(Object key);
    L createSequenceNode();
    M createMapNode();
}
