package io.github.qishr.cascara.common.lang.type;

import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;
import io.github.qishr.cascara.common.lang.plain.PlainMapNode;

/// Handles serialization and deserialization of [Throwable] instances across AST nodes,
/// preserving class names, cause chains, suppressed exceptions, and stack traces.
public class ThrowableTypeSerializer extends AbstractTypeDescriptor<Throwable> implements TypeSerializer<Throwable> {

    private final StackTraceElementTypeSerializer stackTraceSerializer = new StackTraceElementTypeSerializer();

    public ThrowableTypeSerializer() {
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
    public Throwable deserialize(AstNode astNode) throws SerializerException {
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
                StackTraceElement ste = stackTraceSerializer.deserialize(elementNode);
                if (ste != null) {
                    elements.add(ste);
                }
            }
            throwable.setStackTrace(elements.toArray(new StackTraceElement[0]));
        }

        // Deserialize cause
        if (map.get("cause") instanceof MapAstNode causeNode) {
            throwable.initCause(deserialize(causeNode));
        }

        // Deserialize suppressed
        if (map.get("suppressed") instanceof SequenceAstNode suppressedArray) {
            for (AstNode sNode : suppressedArray.getChildren()) {
                Throwable s = deserialize(sNode);
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