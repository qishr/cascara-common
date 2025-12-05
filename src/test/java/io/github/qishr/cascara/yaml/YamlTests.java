package io.github.qishr.cascara.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.yaml.ast.YamlMap;
import io.github.qishr.cascara.yaml.ast.YamlMappingEntry;
import io.github.qishr.cascara.yaml.ast.YamlNode;
import io.github.qishr.cascara.yaml.ast.YamlScalar;

class YamlTests {

    @Test
    void test_rootLevel_arrayAfterEmptyArray() {
        String yamlString = "name: \"nameval\"\n" +
                            "emptyarray: \n" +
                            "array:\n" +
                            "  - value1\n" +
                            "  - value2\n" +
                            "";
        Yaml yaml = new Yaml(yamlString);
        List<YamlNode> array = yaml.getSequence("array");
        assertEquals(2, array.size());
    }

    @Test
    void test_subLevel_arrayAfterEmptyArray() {
        String yamlString = "object:\n" + //
                            "  emptyarray:\n" + //
                            "  array:\n" + //
                            "    - value1\n" + //
                            "    - value2\n" + //
                            "";
        Yaml yaml = new Yaml(yamlString);
        YamlMap object = yaml.getMap("object");
        List<YamlMappingEntry> entries = object.getEntries();

        YamlMappingEntry entry1 = entries.getFirst();
        YamlNode key1 = entry1.getKey();
        if (key1 instanceof YamlScalar scalar) {
            assertEquals("emptyarray", scalar.asString());
        }

        YamlMappingEntry entry2 = entries.getLast();
        YamlNode key2 = entry2.getKey();
        if (key2 instanceof YamlScalar scalar) {
            assertEquals("array", scalar.asString());
        }
    }
}

