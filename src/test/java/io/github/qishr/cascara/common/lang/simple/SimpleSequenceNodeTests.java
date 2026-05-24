package io.github.qishr.cascara.common.lang.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.util.Properties;


public class SimpleSequenceNodeTests {
    @Test
    public void test1() {
        SimpleSequenceNode seq = new SimpleSequenceNode()
            .add(new SimpleScalarNode("one"))
            .add(new SimpleScalarNode("two"))
            .add(new SimpleScalarNode("three"));

        for (SimpleNode node : seq) {
            System.out.println("Node: " + node);
        }
    }
}
