package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.SequencedCollection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.plain.PlainScalarNode;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;
import io.github.qishr.cascara.common.type.TypeReference;

/// Testing of Serialier with JRE Collection types that map to JSON Schema arrays
public class SerializerTypesCollectionTests extends SerializerTestBase {
    PlainSequenceNode intSequence;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        intSequence = new PlainSequenceNode()
            .add(new PlainScalarNode(42))
            .add(new PlainScalarNode(53))
            .add(new PlainScalarNode(86));
    }

    @Test
    void test_list() {
        List<Integer> r = serializer.fromAst(intSequence, new TypeReference<List<Integer>>(){});
        assertInstanceOf(List.class, r);
        assertEquals(3, r.size());
        assertEquals(42, r.get(0));
        assertEquals(53, r.get(1));
        assertEquals(86, r.get(2));
    }

    @Test
    void test_SequencedCollection() {
        SequencedCollection<Integer> r = serializer.fromAst(intSequence, new TypeReference<SequencedCollection<Integer>>(){});
        assertInstanceOf(List.class, r);
        assertEquals(3, r.size());
        Integer[] array = r.toArray(new Integer[]{});
        assertEquals(42, array[0]);
        assertEquals(53, array[1]);
        assertEquals(86, array[2]);
    }

}
