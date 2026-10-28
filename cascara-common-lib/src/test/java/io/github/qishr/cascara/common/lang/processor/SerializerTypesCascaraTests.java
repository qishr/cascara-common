package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.DiagnosticBuilder;
import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;
import io.github.qishr.cascara.common.diagnostic.message.GenericMessage;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.LangMessage;

/// Testing of Serialier with Cascara common types that map to JSON Schema objects
public class SerializerTypesCascaraTests extends SerializerTestBase {

    @Test
    void test_serialization_GenericMessage() {
        GenericMessage original = GenericMessage.UNIMPLEMENTED_METHOD;
        AstNode ast = serializer.toAst(original);
        assertInstanceOf(ScalarAstNode.class, ast);
        String expected = GenericMessage.class.getName() + "." + "UNIMPLEMENTED_METHOD";
        assertEquals(expected, ((ScalarAstNode<?>)ast).asString());
    }

    // This was failing because LangMessage is being treated as a regular Enum during
    // serialization, but DiagnosticMessage uses a TypeDescriptor during deserialization.
    @Test
    void test_DiagnosticMessage() {
        DiagnosticMessage original = LangMessage.AMBIGUOUS_POLYMORPHIC;
        AstNode ast = serializer.toAst(original);
        DiagnosticMessage deserialized = serializer.fromAst(ast, DiagnosticMessage.class);
        assertEquals("LANG-316", deserialized.getCode());
    }

    // This was only working because the serializer was treating GenericMessage as a regular Enum.
    // It should have been be using a TypeDescriptor instead.

    // The problem was that getTypeDescriptor() only matched an exact class whereas it should
    // Try that first, then work its way up through the superclass chain.

    // This gets close to the idea that AbstractSerializer should just be a
    // TypeSerializer<Object>, but thats a philosophical discussion for another day.
    // The mere existence of a TypeSerializer<Object> is likely to break everything downstream.
    @Test
    void test_GenericMessage() {
        GenericMessage original = GenericMessage.UNIMPLEMENTED_METHOD;
        AstNode ast = serializer.toAst(original);
        GenericMessage deserialized = serializer.fromAst(ast, GenericMessage.class);
        assertEquals("ERROR-102", deserialized.getCode());
    }

    // getTypeDescriptor(jvmInstance.getClass() was failing in this test because
    // it was looking for an exact match on GenericMessage instead of checking
    // for a matching supertype DiagnosticMessage.
    @Test
    void test_Diagnostic_withStackTrace() {

        StackTraceElement[] stackTrace = new StackTraceElement[]{
            new StackTraceElement("className1", "methodName1", "fileName1", 1)
        };

        Throwable cause = null;

        Diagnostic original = DiagnosticBuilder.build(
            "className",
            Level.WARN,
            stackTrace,
            cause,
            GenericMessage.WARN,
            "testWarning"
        );

        AstNode ast = serializer.toAst(original);
        Diagnostic deserialized = serializer.fromAst(ast, Diagnostic.class);

        assertEquals(Level.WARN, deserialized.getLevel());
        assertEquals("testWarning", deserialized.getFormattedMessage());
        assertEquals("WARN-101", deserialized.getMessage().getCode());
        assertEquals(1, deserialized.getStackTrace().length);
        StackTraceElement element = deserialized.getStackTrace()[0];
        assertEquals("className1", element.getClassName());
        assertEquals("methodName1", element.getMethodName());
    }
}
