package io.github.qishr.cascara.common.lang;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.exception.SerializerException;
import io.github.qishr.cascara.common.lang.processor.AbstractSerializer;
import io.github.qishr.cascara.common.lang.processor.AstParser;
import io.github.qishr.cascara.common.lang.reference.ReferenceMapEntryNode;
import io.github.qishr.cascara.common.lang.reference.ReferenceMapNode;
import io.github.qishr.cascara.common.lang.reference.ReferenceNode;
import io.github.qishr.cascara.common.lang.reference.ReferenceNodeFactory;
import io.github.qishr.cascara.common.lang.reference.ReferenceScalarNode;
import io.github.qishr.cascara.common.lang.reference.ReferenceSequenceNode;
import io.github.qishr.cascara.common.lang.type.TypeReference;
import io.github.qishr.cascara.common.lang.util.LanguageOptions;
import io.github.qishr.cascara.common.util.ContentType;

public class TestSerializer extends AbstractSerializer<TestSerializer,ReferenceNode,ReferenceScalarNode,ReferenceSequenceNode,ReferenceMapNode,ReferenceMapEntryNode,ReferenceNode> {

    public TestSerializer() {
        super("", new ReferenceNodeFactory(), null);
    }

    @Override
    protected TestSerializer self() {
        return this;
    }

    @Override
    protected ReferenceNode serializeKey(Object key) {
        return serialize(key);
    }

    @Override
    public ContentType getContentType() {
        return null;
    }

    /// {@inheritDoc}
    @Override
    public TestSerializer setReporter(Reporter reporter) {
        this.reporter = reporter;
        return this;
    }

    /// {@inheritDoc}
    @Override
    public TestSerializer setOptions(LanguageOptions<?> options) {
        return this;
    }

    /// {@inheritDoc}
    @Override
    public TestSerializer setParser(AstParser<ReferenceNode,?> parser) {
        return this;
    }

    /// {@inheritDoc}
    @Override
    public String toText(Object jvmInstance) {
        return null;
    }

    /// {@inheritDoc}
    @Override
    public ReferenceNode toAst(Object jvmInstance) {
        return serialize(jvmInstance);
    }

    /// {@inheritDoc}
    @Override
    public <C> C fromText(String text, Class<C> jvmType) {
        return null;
    }

    public <C> C fromText(String text, TypeReference<C> typeRef) {
        return null;
    }

    /// {@inheritDoc}
    @Override
    public <C> C fromStream(InputStream is, Class<C> jvmType) {
        return null;
    }

    /// {@inheritDoc}
    @Override
    public <C> C fromStream(InputStream is, TypeReference<C> typeRef) {
        return null;
    }

    /// {@inheritDoc}
    @Override
    public <C> C fromAst(ReferenceNode astNode, Class<C> jvmType) {
        return (C) deserialize(astNode, jvmType);
    }

    public <C> C fromAst(ReferenceNode astNode, TypeReference<C> typeRef) {
        return (C) deserialize(astNode, typeRef);
    }

    @Override
    public <C> C fromReader(Reader reader, Class<C> jvmType) throws SerializerException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fromReader'");
    }

    @Override
    public <C> C fromReader(Reader reader, TypeReference<C> typeRef) throws SerializerException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'fromReader'");
    }

    @Override
    public void toWriter(Object jvmInstance, Writer writer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toWriter'");
    }
}