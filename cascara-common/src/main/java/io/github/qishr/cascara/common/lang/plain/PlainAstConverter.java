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


package io.github.qishr.cascara.common.lang.plain;

import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;
import io.github.qishr.cascara.common.annotation.Experimental;
import io.github.qishr.cascara.common.annotation.Nullable;
import io.github.qishr.cascara.common.diagnostic.exception.LocalizableRuntimeException;
import io.github.qishr.cascara.common.diagnostic.exception.UnimplementedMethodException;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.MapEntryAstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.processor.AstConverter;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.lang.util.LanguageOptions;

@Experimental
public class PlainAstConverter implements AstConverter<PlainNode> {
    @Nullable
    public PlainNode fromAst(AstNode ast) {
        if (ast == null) return null;

        if (ast instanceof MapAstNode astMap) {
            PlainMapNode yamlMap = new PlainMapNode();
            for (Object entry : astMap.getEntries()) {
                if (entry instanceof MapEntryAstNode astMapEntry) {
                    String key = astMapEntry.getKeyString();
                    PlainNode yamlValue = fromAst(astMapEntry.getValue());
                    yamlMap.put(key, yamlValue);
                }
            }
            return yamlMap;
        } else if (ast instanceof SequenceAstNode astSeq) {
            PlainSequenceNode yamlSeq = new PlainSequenceNode();
            for (Object element : astSeq.getElements()) {
                if (element instanceof AstNode astElement) {
                    yamlSeq.add(fromAst(astElement));
                }
            }
            return yamlSeq;
        } else if (ast instanceof ScalarAstNode astScalar) {
            PlainScalarNode yamlScalar = new PlainScalarNode(astScalar.getPrimitive());
            return yamlScalar;
        } else {
            String name = (ast == null) ? "null" : ast.getClass().getSimpleName();
            throw new LocalizableRuntimeException(LangMessage.UNKNOWN_NODE_TYPE, name);
        }
    }

    @Nullable
    public PlainNode toPlainAst(PlainNode yaml) {
        if (yaml == null) return null;

        if (yaml instanceof PlainMapNode map) {
            PlainMapNode out = new PlainMapNode();
            for (PlainMapEntryNode e : map.getEntries()) {
                PlainNode key = toPlainAst(e.getKey());
                PlainNode val = toPlainAst(e.getValue());
                out.put(key, val);
            }
            return out;
        }

        if (yaml instanceof PlainSequenceNode seq) {
            PlainSequenceNode out = new PlainSequenceNode();
            for (PlainNode child : seq.getChildren()) {
                out.add(toPlainAst(child));
            }
            return out;
        }

        if (yaml instanceof PlainScalarNode scalar) {
            return convertScalar(scalar);   // tag logic goes here
        }

        throw new LocalizableRuntimeException(
            LangMessage.UNKNOWN_NODE_TYPE,
            yaml.getClass().getSimpleName()
        );
    }

    @Nullable
    private PlainScalarNode convertScalar(PlainScalarNode scalar) {
        if (scalar == null) return null;
        Object value = scalar.getPrimitive();
        return new PlainScalarNode(value);
    }

    @Override
    public PlainAstConverter setReporter(Reporter reporter) {
        return this;
    }

    @Override
    public PlainAstConverter setOptions(LanguageOptions<?> options) {
        return this;
    }

    @Override
    public ContentType getContentType() {
        return null;
    }

    @Override
    public String toString(AstNode ast) {
        throw new UnimplementedMethodException();
    }
}
