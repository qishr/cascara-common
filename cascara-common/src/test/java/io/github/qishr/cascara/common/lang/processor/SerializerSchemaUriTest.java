package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;

public class SerializerSchemaUriTest extends SerializerTestBase {
    public static class SchemaFieldTestClass {
        public static final String SCHEMA = "cascara://core/schema-service/draft/cascara.common/schema-field-test/0.1.0";
    }

    @Test
    void test_staticSchemaFieldIsSerialized() {
        SchemaFieldTestClass sft = new SchemaFieldTestClass();
        AstNode rootNode = serializer.toAst(sft);
        assertInstanceOf(MapAstNode.class, rootNode);
        MapAstNode<?,?,?> rootMap = (MapAstNode<?,?,?>) rootNode;
        String schemaUri = rootMap.getString("$schema");
        assertEquals(SchemaFieldTestClass.SCHEMA, schemaUri);
    }
}
