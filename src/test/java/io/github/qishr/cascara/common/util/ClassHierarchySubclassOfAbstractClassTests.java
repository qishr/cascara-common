package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ClassHierarchySubclassOfAbstractClassTests extends ClassHierarchyTestBase {

    public static abstract class TestBase {
        public String x;
    }

    public static class TestImpl extends TestBase {
        public String y;
    }

    @Test
    void test_subclassDeserialization() {
        List<String> subclasses = ClassHierarchy.getSubclasses(TestBase.class.getName());
        assertNotNull(subclasses);
        assertEquals(1, subclasses.size());
    }
}
