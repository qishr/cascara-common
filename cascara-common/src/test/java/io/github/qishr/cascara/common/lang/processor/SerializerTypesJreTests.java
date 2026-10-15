package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;

/// Testing of Serialier with JRE types that map to JSON Schema objects
public class SerializerTypesJreTests extends SerializerTestBase {
    @Test
    void test_serializeStackTraceElement() {
        StackTraceElement original = new StackTraceElement("className1", "methodName1", "fileName1", 1);

        AstNode ast = serializer.toAst(original);
        StackTraceElement ste = serializer.fromAst(ast, StackTraceElement.class);

        assertEquals("className1", ste.getClassName());
    }
}
