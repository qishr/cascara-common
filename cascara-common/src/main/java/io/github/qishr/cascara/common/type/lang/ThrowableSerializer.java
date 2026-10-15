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

package io.github.qishr.cascara.common.type.lang;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.diagnostic.report.Reporter;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;
import io.github.qishr.cascara.common.type.AbstractTypeDescriptor;
import io.github.qishr.cascara.common.type.PrimitiveType;
import io.github.qishr.cascara.common.type.TypeSerializer;
import io.github.qishr.cascara.common.lang.plain.PlainMapNode;

/// Handles serialization and deserialization of [Throwable] instances across AST nodes,
/// preserving class names, cause chains, suppressed exceptions, and stack traces.
public class ThrowableSerializer extends AbstractTypeDescriptor<Throwable> implements TypeSerializer<Throwable> {

    private final StackTraceElementSerializer stackTraceSerializer = new StackTraceElementSerializer();

    public ThrowableSerializer() {
        super(Throwable.class, PrimitiveType.STRING);
    }

    @Override
    public PlainMapNode serialize(Throwable jvmInstance) throws SerializerException {
        if (jvmInstance == null) {
            return null;
        }

        PlainMapNode node = new PlainMapNode()
            .put("type", jvmInstance.getClass().getName())
            .put("message", jvmInstance.getMessage());

        // Serialize stack trace elements
        PlainSequenceNode stackArray = new PlainSequenceNode();
        for (StackTraceElement element : jvmInstance.getStackTrace()) {
            stackArray.add(stackTraceSerializer.serialize(element));
        }
        node.put("stackTrace", stackArray);

        // Recursive cause
        if (jvmInstance.getCause() != null && jvmInstance.getCause() != jvmInstance) {
            node.put("cause", serialize(jvmInstance.getCause()));
        }

        // Suppressed exceptions
        Throwable[] suppressed = jvmInstance.getSuppressed();
        if (suppressed != null && suppressed.length > 0) {
            PlainSequenceNode suppressedArray = new PlainSequenceNode();
            for (Throwable s : suppressed) {
                suppressedArray.add(serialize(s));
            }
            node.put("suppressed", suppressedArray);
        }

        return node;
    }

    @Override
    public Throwable deserialize(AstNode astNode, Type targetType) throws SerializerException {
        if (!(astNode instanceof MapAstNode map)) {
            return null;
        }

        String type = map.getString("type");
        String message = map.getString("message");

        SerializedThrowable throwable = new SerializedThrowable(type, message);

        // Deserialize stack trace
        if (map.get("stackTrace") instanceof SequenceAstNode stackArray) {
            List<StackTraceElement> elements = new ArrayList<>();
            for (AstNode elementNode : stackArray.getChildren()) {
                StackTraceElement ste = stackTraceSerializer.deserialize(elementNode, targetType);
                if (ste != null) {
                    elements.add(ste);
                }
            }
            throwable.setStackTrace(elements.toArray(new StackTraceElement[0]));
        }

        // Deserialize cause
        if (map.get("cause") instanceof MapAstNode causeNode) {
            throwable.initCause(deserialize(causeNode, targetType));
        }

        // Deserialize suppressed
        if (map.get("suppressed") instanceof SequenceAstNode suppressedArray) {
            for (AstNode sNode : suppressedArray.getChildren()) {
                Throwable s = deserialize(sNode, targetType);
                if (s != null) {
                    throwable.addSuppressed(s);
                }
            }
        }

        return throwable;
    }

    @Override
    public boolean validate(String text, Reporter collector) {
        return true;
    }

    /// Synthetic [Throwable] wrapper representing deserialized exceptions from dynamic
    /// module layers without triggering [ClassNotFoundException] on the host classpath.
    public static class SerializedThrowable extends Throwable {
        private final String originalClassName;

        public SerializedThrowable(String originalClassName, String message) {
            super(message);
            this.originalClassName = originalClassName;
        }

        public String getOriginalClassName() {
            return originalClassName;
        }

        @Override
        public String toString() {
            String s = originalClassName != null ? originalClassName : getClass().getName();
            String message = getLocalizedMessage();
            return (message != null) ? (s + ": " + message) : s;
        }
    }
}