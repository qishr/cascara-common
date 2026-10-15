package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.CharBuffer;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;

/// Testing of Serialier with JVM types that map directly to JSON Schema arrays of scalar types
public class SerializerTypesArrayTests extends SerializerTestBase {
    public static class JavaPrimitiveTest {
        public long[]    p_long;
        public int[]     p_int;
        public double[]  p_double;
        public float[]   p_float;
        public short[]   p_short;
    }

    @Test
    void test_primitiveClassMembers() {
        JavaPrimitiveTest types = new JavaPrimitiveTest();
        types.p_long = new long[]{42L};
        types.p_int = new int[]{53};
        types.p_float = new float[]{12.011f};
        types.p_double = new double[]{15.999};
        types.p_short = new short[]{4};

        AstNode root = serializer.toAst(types);
        JavaPrimitiveTest r = serializer.fromAst(root, JavaPrimitiveTest.class);

        assertEquals(types.p_long[0], r.p_long[0]);
        assertEquals(types.p_int[0], r.p_int[0]);
        assertEquals(types.p_float[0], r.p_float[0]);
        assertEquals(types.p_double[0], r.p_double[0]);
        assertEquals(types.p_short[0], r.p_short[0]);
    }

    public static class JavaReferenceTest {
        public Long[]    p_long;
        public Integer[] p_integer;
        public Double[]  p_double;
        public Float[]   p_float;
        public Short[]   p_short;
        public String[]  p_string;
        public CharSequence[]  p_charSequence;
        public CharBuffer[]    p_charBuffer;
        public StringBuffer[]  p_stringBuffer;
        public StringBuilder[] p_stringBuilder;
    }

    @Test
    void test_referenceClassMembers() {
        JavaReferenceTest types = new JavaReferenceTest();
        types.p_long = new Long[]{42L};
        types.p_integer = new Integer[]{53};
        types.p_float = new Float[]{12.011f};
        types.p_double = new Double[]{15.999};
        types.p_short = new Short[]{4};
        types.p_string = new String[]{"banana"};
        types.p_charSequence = new CharSequence[]{"avocado"};
        types.p_charBuffer = new CharBuffer[]{CharBuffer.wrap("kiwi")};
        types.p_stringBuffer = new StringBuffer[]{new StringBuffer("watermelon")};
        types.p_stringBuilder = new StringBuilder[]{new StringBuilder("rubiaceae")};

        AstNode root = serializer.toAst(types);
        JavaReferenceTest r = serializer.fromAst(root, JavaReferenceTest.class);

        assertEquals(types.p_long[0], r.p_long[0]);
        assertEquals(types.p_integer[0], r.p_integer[0]);
        assertEquals(types.p_float[0], r.p_float[0]);
        assertEquals(types.p_double[0], r.p_double[0]);
        assertEquals(types.p_short[0], r.p_short[0]);
        assertEquals(types.p_string[0], r.p_string[0]);
        assertEquals(types.p_charSequence[0], r.p_charSequence[0]);
        assertEquals(types.p_charBuffer[0], r.p_charBuffer[0]);
        assertEquals(types.p_stringBuffer[0], r.p_stringBuffer[0]);
        assertEquals(types.p_stringBuilder[0], r.p_stringBuilder[0]);
    }
}
