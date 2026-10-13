package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.common.lang.type.TypeReference;

public class SerializerSetTest extends SerializerTestBase {
    // TODO
    @Test
    void test_serializeSet() {
        Set<String> o = new HashSet<>();
        o.add("hello");

        AstNode root = serializer.toAst(o);
        assertInstanceOf(SequenceAstNode.class, root);

        Set<String> r = serializer.fromAst(root, new TypeReference<Set<String>>(){});
        assertEquals(o, r);
    }
}
