package io.github.qishr.cascara.common.property;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

public class PropertyTest {
    @Test
    void testTrackableProperty() {
        Property<String> property = new Property<>("name");
        property.setValue("value");
        assertEquals("value", property.getValue());
    }

    @Test
    void testStringProperty() {
        StringProperty property = new StringProperty("name");
        property.setValue("value");
        assertEquals("value", property.getValue());
    }

    @Test
    void testIntegerProperty() {
        Integer four = 4;
        IntegerProperty property = new IntegerProperty("name", four);
        property.setValue(3);
        assertEquals(3, property.getValue());
    }

    @Test
    void testUriProperty() {
        URI uri = URI.create("https://github.com");
        Property<URI> property = new Property<>("name", null);
        property.setValue(uri);
        assertEquals(uri, property.getValue());
    }
}
