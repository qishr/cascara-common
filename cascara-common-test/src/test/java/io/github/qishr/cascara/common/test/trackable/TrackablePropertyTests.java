package io.github.qishr.cascara.common.test.trackable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.property.Property;
import io.github.qishr.cascara.common.trackable.property.TrackableIntegerProperty;
import io.github.qishr.cascara.common.trackable.property.TrackableStringProperty;
import io.github.qishr.cascara.common.trackable.property.TrackableProperty;

public class TrackablePropertyTests {
    @Test
    void testTrackableProperty() {
        TrackableProperty<String> property = new TrackableProperty<>(null, "name", "");
        property.setValue("value");
        assertEquals("value", property.getValue());
    }

    @Test
    void testStringProperty() {
        TrackableStringProperty property = new TrackableStringProperty(null, "name", "");
        property.setValue("value");
        assertEquals("value", property.getValue());
    }

    @Test
    void testIntegerProperty() {
        Integer four = 4;
        TrackableIntegerProperty property = new TrackableIntegerProperty(null, "name", four);
        property.setValue(3);
        assertEquals(3, property.getValue());
    }

    @Test
    void testUriProperty() {
        URI uri = URI.create("https://github.com");
        Property<URI> property = new Property<>( "name", null);
        property.setValue(uri);
        assertEquals(uri, property.getValue());
    }
}
