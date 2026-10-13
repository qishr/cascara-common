package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.type.TypeReference;

public class SerializerOptionalTests extends SerializerTestBase {
    @Test
    void test_optionalInt() {
        OptionalInt o = OptionalInt.of(42);

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        OptionalInt r = serializer.fromAst(root, OptionalInt.class);
        assertEquals(o, r);
    }

    @Test
    void test_optionalLong() {
        OptionalLong o = OptionalLong.of(53);

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        OptionalLong r = serializer.fromAst(root, OptionalLong.class);
        assertEquals(o, r);
    }

    @Test
    void test_optionalDouble() {
        OptionalDouble o = OptionalDouble.of(12.011);

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        OptionalDouble r = serializer.fromAst(root, OptionalDouble.class);
        assertEquals(o, r);
    }

    @Test
    void test_optionalString() {
        Optional<String> o = Optional.of("hello");

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        Optional<String> r = serializer.fromAst(root, new TypeReference<Optional<String>>(){});
        assertEquals(o, r);
    }

    @Test
    void test_optionalList() {
        Optional<List<String>> o = Optional.of(List.of("hello"));

        AstNode root = serializer.toAst(o);
        assertInstanceOf(SequenceAstNode.class, root);

        Optional<List<String>> r = serializer.fromAst(root, new TypeReference<Optional<List<String>>>(){});
        assertEquals(o, r);
    }
}
