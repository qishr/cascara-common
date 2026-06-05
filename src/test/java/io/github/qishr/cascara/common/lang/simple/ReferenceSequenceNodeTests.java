package io.github.qishr.cascara.common.lang.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.reference.ReferenceNode;
import io.github.qishr.cascara.common.lang.reference.ReferenceScalarNode;
import io.github.qishr.cascara.common.lang.reference.ReferenceSequenceNode;


public class ReferenceSequenceNodeTests {
    @Test
    public void test1() {
        ReferenceSequenceNode seq = new ReferenceSequenceNode()
            .add(new ReferenceScalarNode("one"))
            .add(new ReferenceScalarNode("two"))
            .add(new ReferenceScalarNode("three"));

        for (ReferenceNode node : seq) {
            System.out.println("Node: " + node);
        }
    }
}
