package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.annotation.PolymorphicType;
import io.github.qishr.cascara.common.annotation.SubType;
import io.github.qishr.cascara.common.lang.generic.GenericAstMap;
import io.github.qishr.cascara.common.lang.generic.GenericAstScalar;
import io.github.qishr.cascara.common.lang.generic.GenericAstSequence;
import io.github.qishr.cascara.common.type.TypeReference;

public class DeserializeParameterizedTypesTest extends SerializerTestBase {

    @PolymorphicType(property = "type")
    public static class Box<T> {
        public T content;
        public Box() {}
    }

    @SubType(name = "specialized0")
    public static class SpecializedBox0<T> extends Box<T> {
        public String extraMeta;
        public SpecializedBox0() {}
    }

    @SubType(name = "specialized1")
    public static class SpecializedBox1<T> extends Box<T> {
        public String extraMeta;
        public SpecializedBox1() {}
    }

    @Test
    void test_parameterizedSubclassDeserialization() {

        GenericAstMap ast0 = new GenericAstMap()
            .put("type", "specialized1")
            .put("content", "secret")
            .put("extraMeta", "metadata");

        // Using a parameterized subclass of a generic base triggers the specialized parameterized code path
        Box<String> box = serializer.fromAst(ast0, new TypeReference<Box<String>>(){});

        assertInstanceOf(SpecializedBox1.class, box);
        assertEquals("secret", box.content);

        SpecializedBox1<?> specializeBox = (SpecializedBox1<?>) box;
        assertEquals("metadata", specializeBox.extraMeta);
    }




    public static class SpecializedList<T> extends ArrayList<T> {
        public String extraMeta;
        public SpecializedList() {}
    }

    @Test
    void test_parameterizedSubclassOfArrayListDeserialization() {

        GenericAstSequence ast = new GenericAstSequence()
            .add(new GenericAstScalar("item"));

        SpecializedList<String> list = serializer.fromAst(ast, new TypeReference<SpecializedList<String>>(){});

        assertInstanceOf(SpecializedList.class, list);
        assertEquals("item", list.getFirst());
    }
}
