package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;

public class SerializerPrimitiveTests extends SerializerTestBase {
    @Test
    void test_Integer() {
        Integer o = Integer.valueOf(42);

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        Integer r = serializer.fromAst(root, Integer.class);
        assertEquals(o, r);
    }

    @Test
    void test_Long() {
        Long o = Long.valueOf(53);

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        Long r = serializer.fromAst(root, Long.class);
        assertEquals(o, r);
    }

    @Test
    void test_Float() {
        Float o = Float.valueOf(12.011f);

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        Float r = serializer.fromAst(root, Float.class);
        assertEquals(o, r);
    }

    @Test
    void test_Double() {
        Double o = Double.valueOf(15.999);

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        Double r = serializer.fromAst(root, Double.class);
        assertEquals(o, r);
    }
}
