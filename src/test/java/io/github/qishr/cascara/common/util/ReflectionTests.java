package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ReflectionTests {
    public Map<String,List<Double>> field0 = new HashMap<>();
    public List<Double> field1 = new ArrayList<>();

    @Test
    void test_getGenericTypeOfMapValue() throws NoSuchFieldException {
        Field field0 = ReflectionTests.class.getField("field0");
        // Class<?> typeParam1Type = ReflectionUtils.getGenericTypeOfMapValue(field0);
        // assertNotNull(typeParam1Type);
        // assertEquals("List", typeParam1Type.getSimpleName());

        // Class<?> targetType = field0.getType();
        // assertFalse(List.class.isAssignableFrom(targetType));
    }

    // // The type of an item in the list
    // @Test
    // void test_getGenericTypeOfListField() throws NoSuchFieldException {
    //     Field field1 = ReflectionTests.class.getField("field1");
    //     Class<?> typeParam1Type = ReflectionUtils.getGenericTypeOfListField(field1);
    //     assertNotNull(typeParam1Type);
    //     assertEquals("Double", typeParam1Type.getSimpleName());

    //     Class<?> targetType = field1.getType();
    //     assertTrue(List.class.isAssignableFrom(targetType));
    // }
}
