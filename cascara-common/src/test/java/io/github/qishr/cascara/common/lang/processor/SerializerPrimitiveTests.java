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

    @Test
    void test_Short() {
        Short o = Short.valueOf("4");

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        Short r = serializer.fromAst(root, Short.class);
        assertEquals(o, r);
    }

    @Test
    void test_Byte() {
        Byte o = Byte.valueOf("1");

        AstNode root = serializer.toAst(o);
        assertInstanceOf(ScalarAstNode.class, root);

        Byte r = serializer.fromAst(root, Byte.class);
        assertEquals(o, r);
    }

    public static class JavaPrimitiveTest {
        public long    p_long;
        public int     p_integer;
        public double  p_double;
        public float   p_float;
        public short   p_short;
        public byte    p_byte;
        public byte[]  p_bytes;
    }

    public static class JavaReferenceTest {
        public String  p_string;
        public Long    p_long;
        public Integer p_integer;
        public Double  p_double;
        public Float   p_float;
        public Short   p_short;
        public byte    p_byte;
        public Byte[]  p_bytes;
    }

    @Test
    void test_primitiveClassMembers() {
        JavaPrimitiveTest types = new JavaPrimitiveTest();
        types.p_long = 42L;
        types.p_integer = 53;
        types.p_float = 1.2f;
        types.p_double = 1.3;
        types.p_short = 4;
        types.p_byte = 0x3;
        types.p_bytes = new byte[]{5};

        AstNode root = serializer.toAst(types);

        JavaPrimitiveTest r = serializer.fromAst(root, JavaPrimitiveTest.class);

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

    @Test
    void test_referenceClassMembers() {
        JavaReferenceTest types = new JavaReferenceTest();
        types.p_string = "test";
        types.p_long = 42L;
        types.p_integer = 53;
        types.p_float = 1.2f;
        types.p_double = 1.3;
        types.p_short = 4;
        types.p_byte = 0x3;
        types.p_bytes = new Byte[]{5};

        AstNode root = serializer.toAst(types);

        JavaReferenceTest r = serializer.fromAst(root, JavaReferenceTest.class);

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
