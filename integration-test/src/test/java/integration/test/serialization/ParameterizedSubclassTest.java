package integration.test.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.type.TypeReference;

public class ParameterizedSubclassTest extends SerializerTestBase {

    public static class WildcardContainer {
        public java.util.List<?> wildcardList;
        public WildcardContainer() {}
    }

    @Test
    void test_wildcardTypeDeserialization() {
        String json = "{\"wildcardList\": [\"item1\", \"item2\"]}";

        WildcardContainer result = serializer.fromString(json, new TypeReference<WildcardContainer>(){});

        assertNotNull(result);
        assertNotNull(result.wildcardList);
        assertEquals(2, result.wildcardList.size());
        assertEquals("item1", result.wildcardList.get(0));
    }

    // public static class GenericHolder<T> {
        //     public T value;
        //     public GenericHolder() {}
        // }

        // @Test
        // void test_wildcardAndTypeVariableDeserialization() throws Exception {
        //     // Target a wildcard type via reflection on a field or method signature
        //     java.lang.reflect.Field field = GenericHolder.class.getField("value");
        //     Type wildcardFieldType = field.getGenericType(); // This is a TypeVariable (T)

        //     PlainMapNode node = new PlainMapNode().put("value", new PlainScalarNode("hello-wildcard"));

        //     // This exercises the TypeVariable / WildcardType branch in deserialize()
        //     Object result = serializer.fromAst(new PlainScalarNode("direct-wildcard"), wildcardFieldType);
        //     assertEquals("direct-wildcard", result);
        // }

        // @Test
        // void test_explicitWildcardType() throws Exception {
        //     java.lang.reflect.Method method = this.getClass().getMethod("dummyMethod", java.util.List.class);
        //     Type wildcardType = method.getGenericParameterTypes()[0]; // List<?> contains a WildcardType

        //     PlainSequenceNode seq = new PlainSequenceNode().add(new PlainScalarNode("item"));
        //     Object res = serializer.fromAst(seq, wildcardType);
        //     assertNotNull(res);
        // }

        // public void dummyMethod(java.util.List<?> list) {}


}
