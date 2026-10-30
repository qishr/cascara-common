package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.annotation.SchemaDefinition;
import io.github.qishr.cascara.common.service.SplSignaturePolicy;

public class AnnotatedClassesTest extends ClassHierarchyTestBase {
    @Test
    void test_anno() {
        Set<String> schemaClasses = Classes.Annotated.with(SchemaDefinition.class);
        assertTrue(schemaClasses.contains(SplSignaturePolicy.class.getName()));
    }
}
