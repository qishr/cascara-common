package io.github.qishr.cascara.common.lang;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.reference.ReferenceScalarNode;
import io.github.qishr.cascara.common.lang.reference.ReferenceSequenceNode;
import io.github.qishr.cascara.common.lang.type.TypeReference;

public class SerializerTest {
    @Test
    void test() {

        ReferenceScalarNode s1 = new ReferenceScalarNode("one");
        ReferenceScalarNode s2 = new ReferenceScalarNode("two");

        ReferenceSequenceNode swq = new ReferenceSequenceNode()
            .add(s1)
            .add(s2);

        TestSerializer serializer = new TestSerializer();

        List<String> list = serializer.fromAst(swq, new TypeReference<List<String>>() {});

        assertNotNull(list);
    }

}
