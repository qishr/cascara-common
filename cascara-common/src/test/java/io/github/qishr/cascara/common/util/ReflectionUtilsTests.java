package io.github.qishr.cascara.common.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.processor.SerializerEnumTests.BerryEnum;

public class ReflectionUtilsTests {
    public static class TestTypes {
        public BerryEnum p_enum;
        public Enum<BerryEnum> p_enumEnum;
        public Enum<?> p_wildcardEnum;
        // public List<?> p_wildcardList;
        // public List<BerryEnum> p_enumList;
        // public List<? extends BerryEnum> p_extendsEnumList;
        // public List<? extends List<?>> p_extendsListList;
        // public List<List<?>> p_listList;
        // public List<Enum<?>> p_wildcardEnumList;
        // public EnumInterface p_interface;
    }

    @Test
    void test_valid_enum() throws NoSuchFieldException {
        Field f = TestTypes.class.getField("p_enum");
        Type result = ReflectionUtils.getEnumType(f.getGenericType());
        assertTrue(result != null);
    }

    @Test
    void test_valid_enumEnum() throws NoSuchFieldException {
        Field f = TestTypes.class.getField("p_enumEnum");
        Type result = ReflectionUtils.getEnumType(f.getGenericType());
        assertTrue(result != null);
    }

    @Test
    void test_valid_wildcardEnum() throws NoSuchFieldException {
        Field f = TestTypes.class.getField("p_wildcardEnum");
        Type result = ReflectionUtils.getEnumType(f.getGenericType());
        assertTrue(result != null);
    }

    // @Test
    // void test_valid_extendsEnumList() throws NoSuchFieldException {
    //     Field f = TestTypes.class.getField("p_extendsEnumList");
    //     if (f.getGenericType() instanceof ParameterizedType pt) {
    //         Type param0 = pt.getActualTypeArguments()[0];
    //         assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getEnumType(param0));
    //     }
    // }

    // @Test
    // void test_invalid_extendsListList() throws NoSuchFieldException {
    //     Field f = TestTypes.class.getField("p_extendsListList");
    //     if (f.getGenericType() instanceof ParameterizedType pt) {
    //         Type param0 = pt.getActualTypeArguments()[0];
    //         assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getEnumType(param0));
    //     }
    // }

    // @Test
    // void test_invalid_listList() throws NoSuchFieldException {
    //     Field f = TestTypes.class.getField("p_listList");
    //     if (f.getGenericType() instanceof ParameterizedType pt) {
    //         Type param0 = pt.getActualTypeArguments()[0];
    //         assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getEnumType(param0));
    //     }
    // }

    // @Test
    // void test_invalid_enumList() throws NoSuchFieldException {
    //     Field f = TestTypes.class.getField("p_enumList");
    //     assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getEnumType(f.getGenericType()));
    // }

    // @Test
    // void test_invalid_wildcardList() throws NoSuchFieldException {
    //     Field f = TestTypes.class.getField("p_wildcardList");
    //     assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getEnumType(f.getGenericType()));
    // }

    // @Test
    // void test_invalid_wildcardEnumList() throws NoSuchFieldException {
    //     Field f = TestTypes.class.getField("p_wildcardEnumList");
    //     assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getEnumType(f.getGenericType()));
    // }

    // @Test
    // void test_invalid_interface() throws NoSuchFieldException {
    //     Field f = TestTypes.class.getField("p_interface");
    //     assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getEnumType(f.getGenericType()));
    // }

}
