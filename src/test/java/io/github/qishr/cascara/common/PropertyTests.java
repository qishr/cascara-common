package io.github.qishr.cascara.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PropertyTests {

    @Test
    void test_stringValue() {
        Property property = new Property("key");
        property.setValue("value");

        assertEquals("value", property.getString());
    }

    @Test
    void test_booleanValue() {
        Property property = new Property("key");
        property.setValue(true);

        assertEquals(true, property.getBoolean());
    }

}
