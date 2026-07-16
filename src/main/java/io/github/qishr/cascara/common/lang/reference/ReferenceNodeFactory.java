package io.github.qishr.cascara.common.lang.reference;

import io.github.qishr.cascara.common.lang.ast.AstNodeFactory;
import io.github.qishr.cascara.common.lang.util.LanguageOptions;
import io.github.qishr.cascara.common.lang.util.QuoteStyle;

public class ReferenceNodeFactory implements AstNodeFactory<ReferenceNode,ReferenceScalarNode,ReferenceSequenceNode,ReferenceMapNode,ReferenceMapEntryNode,ReferenceNode> {

    @Override
    public ReferenceScalarNode createScalarNode(Object jvmValue) {
        return new ReferenceScalarNode(jvmValue);
    }

    @Override
    public ReferenceScalarNode createScalarNode(Object jvmValue, QuoteStyle quoteStyle) {
        return new ReferenceScalarNode(jvmValue, quoteStyle);
    }

	@Override
	public ReferenceScalarNode createScalarNode(Object jvmValue, QuoteStyle quoteStyle, LanguageOptions<?> options) {
        return new ReferenceScalarNode(jvmValue, quoteStyle);
	}

    @Override
    public ReferenceScalarNode createKey(Object key) {
        return new ReferenceScalarNode(key);
    }

    @Override
    public ReferenceSequenceNode createSequenceNode() {
        return new ReferenceSequenceNode();
    }

    @Override
    public ReferenceMapNode createMapNode() {
        return new ReferenceMapNode();
    }
}
