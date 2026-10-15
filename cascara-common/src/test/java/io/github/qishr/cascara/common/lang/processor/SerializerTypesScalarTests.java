package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.TimeZone;
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
    void test_lang_primitiveClassMembers() {
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
    void test_lang_referenceClassMembers() {
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
    void test_lang_primitiveArray_TypeDescriptors() {
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
    void test_lang_referenceArray_typeDescriptors() {
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

    public static class JavaNetTypeDescriptorTest {
        public URI p_uri;
    }

    @Test
    void test_net_typeDescriptors() {
        JavaNetTypeDescriptorTest types = new JavaNetTypeDescriptorTest();
        types.p_uri = URI.create("http://example.com");

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        for (MapEntryAstNode<?,?> entry : mapNode.entrySet()) {
            String fieldName = entry.getKeyString();
            AstNode value = entry.getValue();
            assertInstanceOf(ScalarAstNode.class, value, fieldName + " should serialize to a scalar");
        }

        JavaNetTypeDescriptorTest r = serializer.fromAst(root, JavaNetTypeDescriptorTest.class);
        assertEquals(types.p_uri, r.p_uri);
    }

    public static class JavaNioTypeDescriptorTest {
        public Path p_path;
    }

    @Test
    void test_nio_typeDescriptors() {
        JavaNioTypeDescriptorTest types = new JavaNioTypeDescriptorTest();
        types.p_path = Path.of("/tmp");

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        for (MapEntryAstNode<?,?> entry : mapNode.entrySet()) {
            String fieldName = entry.getKeyString();
            AstNode value = entry.getValue();
            assertInstanceOf(ScalarAstNode.class, value, fieldName + " should serialize to a scalar");
        }

        JavaNioTypeDescriptorTest r = serializer.fromAst(root, JavaNioTypeDescriptorTest.class);
        assertEquals(types.p_path, r.p_path);
    }

    public static class JavaTimeTypeDescriptorTest {
        public Instant p_instant;
        public LocalDateTime p_localDateTime;
        public ZonedDateTime p_zonedDateTime;
        public Date p_date;
        public TimeZone p_timeZone;
        public Duration p_duration;
        public Period p_period;
        public LocalTime p_localTime;
        public OffsetDateTime p_offsetDateTime;
        public OffsetTime p_offsetTime;
        public ZoneId p_zoneId;
        public ZoneOffset p_zoneOffset;
    }

    @Test
    void test_time_typeDescriptors() {
        JavaTimeTypeDescriptorTest types = new JavaTimeTypeDescriptorTest();
        types.p_instant = Instant.now();
        types.p_localDateTime = LocalDateTime.now();
        types.p_zonedDateTime = ZonedDateTime.now();
        types.p_date = new Date();
        types.p_timeZone = TimeZone.getTimeZone("UTC");
        types.p_duration = Duration.ofHours(2);
        types.p_period = Period.ofDays(5);
        types.p_localTime = LocalTime.of(13, 45);
        types.p_offsetDateTime = OffsetDateTime.now();
        types.p_offsetTime = OffsetTime.now();
        types.p_zoneId = ZoneId.of("Europe/London");
        types.p_zoneOffset = ZoneOffset.ofHours(1);

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        for (MapEntryAstNode<?,?> entry : mapNode.entrySet()) {
            String fieldName = entry.getKeyString();
            AstNode value = entry.getValue();
            assertInstanceOf(ScalarAstNode.class, value, fieldName + " should serialize to a scalar");
        }

        JavaTimeTypeDescriptorTest r = serializer.fromAst(root, JavaTimeTypeDescriptorTest.class);
        assertEquals(types.p_instant, r.p_instant);
        assertEquals(types.p_localDateTime, r.p_localDateTime);
        assertEquals(types.p_zonedDateTime, r.p_zonedDateTime);
        assertEquals(types.p_date, r.p_date);
        assertEquals(types.p_timeZone, r.p_timeZone);
        assertEquals(types.p_duration, r.p_duration);
        assertEquals(types.p_period, r.p_period);
        assertEquals(types.p_localTime, r.p_localTime);
        assertEquals(types.p_offsetDateTime, r.p_offsetDateTime);
        assertEquals(types.p_offsetTime, r.p_offsetTime);
        assertEquals(types.p_zoneId, r.p_zoneId);
        assertEquals(types.p_zoneOffset, r.p_zoneOffset);
    }

    public static class JavaUtilTypeDescriptorTest {
        public UUID p_uuid;
    }

    @Test
    void test_util_typeDescriptors() {
        JavaUtilTypeDescriptorTest types = new JavaUtilTypeDescriptorTest();
        types.p_uuid = UUID.randomUUID();

        AstNode root = serializer.toAst(types);
        assertInstanceOf(MapAstNode.class, root);
        MapAstNode<?, ?, ?> mapNode = (MapAstNode<?, ?, ?>) root;

        for (MapEntryAstNode<?,?> entry : mapNode.entrySet()) {
            String fieldName = entry.getKeyString();
            AstNode value = entry.getValue();
            assertInstanceOf(ScalarAstNode.class, value, fieldName + " should serialize to a scalar");
        }

        JavaUtilTypeDescriptorTest r = serializer.fromAst(root, JavaUtilTypeDescriptorTest.class);
        assertEquals(types.p_uuid, r.p_uuid);
    }
}
