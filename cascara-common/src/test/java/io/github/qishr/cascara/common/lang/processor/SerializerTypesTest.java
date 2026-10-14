package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;

public class SerializerTypesTest extends SerializerTestBase {

    public static class TypesTest {
        public String  p_string;

        public Long    p_long;
        public Integer p_integer;
        public Double  p_double;
        public Float   p_float;
        public Short   p_short;
        public byte    p_byte;

        public byte[]  p_bytes;
    }

    @Test
    void test_types() {
        TypesTest types = new TypesTest();
        types.p_string = "test";
        types.p_long = 42L;
        types.p_integer = 53;
        types.p_float = 1.2f;
        types.p_double = 1.3;
        types.p_short = 4;
        types.p_byte = 0x3;
        types.p_bytes = new byte[]{5};

        AstNode root = serializer.toAst(types);

        TypesTest r = serializer.fromAst(root, TypesTest.class);

        assertEquals(types.p_string, r.p_string);
        assertEquals(types.p_long, r.p_long);
        assertEquals(types.p_integer, r.p_integer);
        assertEquals(types.p_float, r.p_float);
        assertEquals(types.p_double, r.p_double);
        assertEquals(types.p_short, r.p_short);
        assertEquals(types.p_byte, r.p_byte);

        // JUnit can't verify if two arrays are the same?
        assertEquals(types.p_bytes.length, r.p_bytes.length);
        assertEquals(types.p_bytes[0], r.p_bytes[0]);
    }
}
