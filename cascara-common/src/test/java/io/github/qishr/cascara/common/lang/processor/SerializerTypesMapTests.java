package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.generic.GenericAstMap;
import io.github.qishr.cascara.common.lang.generic.GenericAstScalar;
import io.github.qishr.cascara.common.lang.generic.GenericAstSequence;
import io.github.qishr.cascara.common.type.TypeReference;

/// Testing of Serialier with JRE Map types that map to JSON Schema objects
public class SerializerTypesMapTests extends SerializerTestBase {
    @Test
    void test_mapOfStringTo_Integer() {
        GenericAstMap map = new GenericAstMap()
            .put("k", new GenericAstScalar(42));

        Map<String,Integer> r = serializer.fromAst(map, new TypeReference<Map<String,Integer>>(){});
        assertInstanceOf(Map.class, r);
    }

    @Test
    void test_mapOfStringTo_ListOfInteger() {
        GenericAstMap map = new GenericAstMap()
            .put("k", new GenericAstSequence()
                .add(
                    new GenericAstScalar(0.1)
                ));

        Map<String,List<Integer>> r = serializer.fromAst(map, new TypeReference<Map<String,List<Integer>>>(){});
        assertInstanceOf(Map.class, r);
        List<Integer> list = r.get("k");
        assertInstanceOf(List.class, list);
    }

    @Test
    void test_mapOfStringTo_ListOfMap() {
        GenericAstMap map = new GenericAstMap()
            .put("k", new GenericAstSequence()
                .add(new GenericAstMap()
                    .put("name", "TestName")
                )
            );

        Map<String,List<Map<String,String>>> r = serializer.fromAst(map, new TypeReference<Map<String,List<Map<String,String>>>>(){});
        assertInstanceOf(Map.class, r);
        List<Map<String,String>> list = r.get("k");
        assertInstanceOf(List.class, list);
        Map<String,String> innerMap = list.getFirst();
        assertInstanceOf(Map.class, innerMap);
        assertEquals("TestName", innerMap.get("name"));
    }

    public static class TestObject {
        public String name;
    }

    @Test
    void test_mapOfStringTo_ListOfObject() {
        GenericAstMap map = new GenericAstMap()
            .put("k", new GenericAstSequence()
                .add(new GenericAstMap()
                    .put("name", "TestName")
                )
            );

        Map<String,List<TestObject>> r = serializer.fromAst(map, new TypeReference<Map<String,List<TestObject>>>(){});
        assertInstanceOf(Map.class, r);
        List<TestObject> list = r.get("k");
        assertInstanceOf(List.class, list);
        TestObject innerMap = list.getFirst();
        assertInstanceOf(TestObject.class, innerMap);
        assertEquals("TestName", innerMap.name);
    }
}
