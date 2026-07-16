// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.


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