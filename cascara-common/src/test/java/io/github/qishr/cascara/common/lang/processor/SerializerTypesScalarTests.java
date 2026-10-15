package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.ast.MapEntryAstNode;
import io.github.qishr.cascara.common.lang.ast.ScalarAstNode;

/// Testing of Serialier with JVM types that map directly to JSON Schema scalars
public class SerializerTypesScalarTests extends SerializerTestBase {
    public static class JavaPrimitiveTest {
        public long    p_long;
        public int     p_int;
        public double  p_double;
        public float   p_float;
        public short   p_short;
        public char    p_char;
        public byte    p_byte;
    }

    @Test
    void test_primitiveClassMembers() {
        JavaPrimitiveTest types = new JavaPrimitiveTest();
        types.p_long = 42L;
        types.p_int = 53;
        types.p_float = 12.011f;
        types.p_double = 15.999;
        types.p_short = 4;
        types.p_byte = 0x3;
        types.p_char = 'c';

        AstNode root = serializer.toAst(types);
        JavaPrimitiveTest r = serializer.fromAst(root, JavaPrimitiveTest.class);

        assertEquals(types.p_long, r.p_long);
        assertEquals(types.p_int, r.p_int);
        assertEquals(types.p_float, r.p_float);
        assertEquals(types.p_double, r.p_double);
        assertEquals(types.p_short, r.p_short);
        assertEquals(types.p_byte, r.p_byte);
        assertEquals(types.p_char, r.p_char);
    }

    public static class JavaReferenceTest {
        public Long    p_long;
        public Integer p_integer;
        public Double  p_double;
        public Float   p_float;
        public Short   p_short;
        public Byte    p_byte;
        public Character p_character;
        public String  p_string;
        public CharSequence  p_charSequence;
        public CharBuffer    p_charBuffer;
        public StringBuffer  p_stringBuffer;
        public StringBuilder p_stringBuilder;
    }

    @Test
    void test_referenceClassMembers() {
        JavaReferenceTest types = new JavaReferenceTest();
        types.p_long = 42L;
        types.p_integer = 53;
        types.p_float = 12.011f;
        types.p_double = 15.999;
        types.p_short = 4;
        types.p_byte = 0x3;
        types.p_character = 'c';
        types.p_string = "banana";
        types.p_charSequence = "avocado";
        types.p_charBuffer = CharBuffer.wrap("kiwi");
        types.p_stringBuffer = new StringBuffer("watermelon");
        types.p_stringBuilder = new StringBuilder("rubiaceae");

        AstNode root = serializer.toAst(types);
        JavaReferenceTest r = serializer.fromAst(root, JavaReferenceTest.class);

        assertEquals(types.p_long, r.p_long);
        assertEquals(types.p_integer, r.p_integer);
        assertEquals(types.p_float, r.p_float);
        assertEquals(types.p_double, r.p_double);
        assertEquals(types.p_short, r.p_short);
        assertEquals(types.p_byte, r.p_byte);
        assertEquals(types.p_character, r.p_character);
        assertEquals(types.p_string, r.p_string);
        assertEquals(types.p_charSequence, r.p_charSequence);
        assertEquals(types.p_charBuffer, r.p_charBuffer);
        assertEquals(types.p_stringBuffer, r.p_stringBuffer);
        assertEquals(types.p_stringBuilder, r.p_stringBuilder);
    }

    public static class JavaPrimitiveArrayTypeDescriptorTest {
        public char[] p_char;
        public byte[] p_byte;
    }

    @Test
    void test_primitiveArrayTypeDescriptors() {
        JavaPrimitiveArrayTypeDescriptorTest types = new JavaPrimitiveArrayTypeDescriptorTest();
        types.p_byte = new byte[]{0x3};
        types.p_char = new char[]{'c'};

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        // Verify 'p_byte' and 'p_char' are serialized as ScalarAstNode
        AstNode byteNode = mapNode.get("p_byte");
        AstNode charNode = mapNode.get("p_char");

        assertInstanceOf(ScalarAstNode.class, byteNode, "p_byte should serialize to a scalar (Base64 string)");
        assertInstanceOf(ScalarAstNode.class, charNode, "p_char should serialize to a scalar string");

        JavaPrimitiveArrayTypeDescriptorTest r = serializer.fromAst(root, JavaPrimitiveArrayTypeDescriptorTest.class);

        assertEquals(types.p_byte[0], r.p_byte[0]);
        assertEquals(types.p_char[0], r.p_char[0]);
    }

    public static class JavaReferenceArrayTypeDescriptorTest {
        public Byte[] p_byte;
        public Character[] p_character;
    }

    @Test
    void test_referenceArrayTypeDescriptors() {
        JavaReferenceArrayTypeDescriptorTest types = new JavaReferenceArrayTypeDescriptorTest();
        types.p_byte = new Byte[]{0x3};
        types.p_character = new Character[]{'c'};

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        // Verify 'p_byte' and 'p_char' are serialized as ScalarAstNode
        AstNode byteNode = mapNode.get("p_byte");
        AstNode charNode = mapNode.get("p_character");

        assertInstanceOf(ScalarAstNode.class, byteNode, "p_byte should serialize to a scalar (Base64 string)");
        assertInstanceOf(ScalarAstNode.class, charNode, "p_character should serialize to a scalar string");

        JavaReferenceArrayTypeDescriptorTest r = serializer.fromAst(root, JavaReferenceArrayTypeDescriptorTest.class);

        assertEquals(types.p_byte[0], r.p_byte[0]);
        assertEquals(types.p_character[0], r.p_character[0]);
    }

    public static class JavaReferenceTypeDescriptorTest {
        public Instant p_instant;
        public LocalDateTime p_localDateTime;
        public Path p_path;
        public URI p_uri;
        public UUID p_uuid;
        public ZonedDateTime p_zonedDateTime;
    }

    @Test
    void test_referenceTypeDescriptors() {
        JavaReferenceTypeDescriptorTest types = new JavaReferenceTypeDescriptorTest();
        types.p_instant = Instant.now();
        types.p_localDateTime = LocalDateTime.now();
        types.p_path = Path.of("/tmp");
        types.p_uri = URI.create("http://example.com");
        types.p_uuid = UUID.randomUUID();
        types.p_zonedDateTime = ZonedDateTime.now();

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        for (MapEntryAstNode<?,?> entry : mapNode.entrySet()) {
            String fieldName = entry.getKeyString();
            AstNode value = entry.getValue();
            assertInstanceOf(ScalarAstNode.class, value, fieldName + " should serialize to a scalar");
        }

        JavaReferenceTypeDescriptorTest r = serializer.fromAst(root, JavaReferenceTypeDescriptorTest.class);
        assertEquals(types.p_instant, r.p_instant);
        assertEquals(types.p_localDateTime, r.p_localDateTime);
        assertEquals(types.p_path, r.p_path);
        assertEquals(types.p_uri, r.p_uri);
        assertEquals(types.p_uuid, r.p_uuid);
        assertEquals(types.p_zonedDateTime, r.p_zonedDateTime);
    }
}
