package io.github.qishr.cascara.common.lang.processor;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.lang.plain.PlainMapNode;
import io.github.qishr.cascara.common.lang.plain.PlainScalarNode;
import io.github.qishr.cascara.common.lang.plain.PlainSequenceNode;
import io.github.qishr.cascara.common.lang.processor.SerializerTests.PackageJsonFile;
import io.github.qishr.cascara.common.lang.type.TypeReference;

public class SerializerCollectionTests extends SerializerTestBase {
    @Test
    void test_list() {
        PlainSequenceNode seq = new PlainSequenceNode()
            .add(new PlainScalarNode(42));

        List<Integer> r = serializer.fromAst(seq, new TypeReference<List<Integer>>(){});
        assertInstanceOf(List.class, r);
    }
}
