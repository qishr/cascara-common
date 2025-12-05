package io.github.qishr.cascara.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PropertiesTests {

    @Test
    void test_string() {
        Properties properties = new Properties();
        properties.set("key", "value");
        assertEquals("value", properties.getString("key"));
    }

    public class TestObject {
        private String value = "";
        public TestObject(String v) {
            value = v;
        }
        public String getValue() {
            return value;
        }
    }
}
