package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.plain.PlainMapNode;
import io.github.qishr.cascara.common.lang.plain.PlainScalarNode;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;
import io.github.qishr.cascara.common.lang.type.TypeReference;

/// Testing of Serialier with JRE Map types that map to JSON Schema objects
public class SerializerTypesMapTests extends SerializerTestBase {
    @Test
    void test_mapOfStringTo_Integer() {
        PlainMapNode map = new PlainMapNode()
            .put("k", new PlainScalarNode(42));

        Map<String,Integer> r = serializer.fromAst(map, new TypeReference<Map<String,Integer>>(){});
        assertInstanceOf(Map.class, r);
    }

    @Test
    void test_mapOfStringTo_ListOfInteger() {
        PlainMapNode map = new PlainMapNode()
            .put("k", new PlainSequenceNode()
                .add(
                    new PlainScalarNode(0.1)
                ));

        Map<String,List<Integer>> r = serializer.fromAst(map, new TypeReference<Map<String,List<Integer>>>(){});
        assertInstanceOf(Map.class, r);
        List<Integer> list = r.get("k");
        assertInstanceOf(List.class, list);
    }

    @Test
    void test_mapOfStringTo_ListOfMap() {
        PlainMapNode map = new PlainMapNode()
            .put("k", new PlainSequenceNode()
                .add(new PlainMapNode()
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
        PlainMapNode map = new PlainMapNode()
            .put("k", new PlainSequenceNode()
                .add(new PlainMapNode()
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
