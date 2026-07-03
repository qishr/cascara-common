package io.github.qishr.cascara.common.lang.reference;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class ReferenceMapNodeTests {
    @Test
    void test() {
        ReferenceMapNode map = new ReferenceMapNode();
        ReferenceScalarNode key = new ReferenceScalarNode("key");
        ReferenceScalarNode value = new ReferenceScalarNode("value");
        map.put(key, value);
        Set<ReferenceMapEntryNode> set = map.entrySet();

    }
}
