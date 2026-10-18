package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.MapAstNode;
import io.github.qishr.cascara.common.lang.diagnostic.SerializerException;

public class SerializerEnumTests extends  SerializerTestBase {
    public static interface EnumInterface {}

    public static enum BerryEnum implements EnumInterface {
        AVOCADO,
        BANANA,
        COFFEE,
        CUCUMBER,
        EGGPLANT,
        GRAPE,
        KIWI,
        PUMPKIN,
        TOMATO,
        WATERMELON
    }

    public static class EnumTest {
        BerryEnum p_enum;
    }

    public static class GenericEnumTest {
        Enum<?> p_enum;
    }

    public static class InterfaceTypeTest {
        Set<EnumInterface> p_set;
    }

    public static class EnumTypeTest {
        Set<BerryEnum> p_set;
        EnumSet<BerryEnum> p_enumSet;
        EnumMap<BerryEnum, String> p_enumMap;
    }

    public static class WildcardTypeTest {
        Set<?> p_set;
        EnumSet<?> p_enumSet;
        EnumMap<?,?> p_enumMap;
    }

    @Test
    void test_enum() {
        EnumTest original = new EnumTest();
        original.p_enum = BerryEnum.COFFEE;

        AstNode node = serializer.toAst(original);

        // Ensure a serialized simple enum field contains only the enum constant name
        assertInstanceOf(MapAstNode.class, node);
        assertEquals("COFFEE", ((MapAstNode<?,?,?>)node).getString("p_enum"));

        EnumTest result = serializer.fromAst(node, EnumTest.class);

        assertEquals(original.p_enum, result.p_enum);
    }

    @Test
    void test_enumGeneric() {
        GenericEnumTest original = new GenericEnumTest();
        original.p_enum = BerryEnum.GRAPE;

        AstNode node = serializer.toAst(original);
        GenericEnumTest result = serializer.fromAst(node, GenericEnumTest.class);

        assertEquals(original.p_enum, result.p_enum);
    }

    @Test
    void test_enumInterfaceType() {
        InterfaceTypeTest original = new InterfaceTypeTest();
        original.p_set = Set.of(BerryEnum.COFFEE);

        AstNode node = serializer.toAst(original);
        InterfaceTypeTest result = serializer.fromAst(node, InterfaceTypeTest.class);

        assertEquals(original.p_set, result.p_set);
    }

    @Test
    void test_enumType() {
        EnumTypeTest original = new EnumTypeTest();
        original.p_enumSet = EnumSet.of(BerryEnum.TOMATO, BerryEnum.WATERMELON);
        original.p_enumMap = new EnumMap<>(BerryEnum.class);
        original.p_enumMap.put(BerryEnum.WATERMELON, "Daiquiri");
        original.p_set = Set.of(BerryEnum.COFFEE);

        AstNode node = serializer.toAst(original);
        EnumTypeTest result = serializer.fromAst(node, EnumTypeTest.class);

        assertEquals(original.p_enumSet, result.p_enumSet);
        assertEquals(original.p_enumMap, result.p_enumMap);
        assertEquals(original.p_set, result.p_set);
    }

    @Test
    void test_specificToWildcard() {
        EnumTypeTest original = new EnumTypeTest();
        original.p_enumSet = EnumSet.of(BerryEnum.COFFEE, BerryEnum.PUMPKIN);
        original.p_enumMap = new EnumMap<>(BerryEnum.class);
        original.p_enumMap.put(BerryEnum.AVOCADO, "Toast");

        AstNode node = serializer.toAst(original);

        // The deserialization target type here is ambiguous: java.util.EnumMap<?,?>
        assertThrows(SerializerException.class, () -> serializer.fromAst(node, WildcardTypeTest.class));
    }

    @Test
    void test_wildcardToWildcard() {
        WildcardTypeTest original = new WildcardTypeTest();
        original.p_enumSet = EnumSet.of(BerryEnum.BANANA, BerryEnum.CUCUMBER);
        EnumMap<BerryEnum,String> spicedLatte = new EnumMap<>(BerryEnum.class);
        spicedLatte.put(BerryEnum.COFFEE, "Java");
        spicedLatte.put(BerryEnum.PUMPKIN, "Kabocha");
        original.p_enumMap = spicedLatte;

        AstNode node = serializer.toAst(original);
        WildcardTypeTest result = serializer.fromAst(node, WildcardTypeTest.class);

        assertEquals(original.p_enumSet, result.p_enumSet);
        assertEquals(original.p_enumMap, result.p_enumMap);
    }
}
