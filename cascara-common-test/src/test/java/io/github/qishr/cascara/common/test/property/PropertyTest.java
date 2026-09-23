package io.github.qishr.cascara.common.test.property;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.property.IntegerProperty;
import io.github.qishr.cascara.common.property.Property;
import io.github.qishr.cascara.common.property.StringProperty;

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
