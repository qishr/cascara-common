package io.github.qishr.cascara.common.lang.reference;

import io.github.qishr.cascara.common.lang.ast.*;
import java.util.Collections;
import java.util.List;

public final class ReferenceMapEntryNode extends ReferenceNode implements MapEntryAstNode<ReferenceNode> {

    private final ReferenceNode key;
    private ReferenceNode value;

    public ReferenceMapEntryNode(ReferenceNode key, ReferenceNode value) {
        this.key = key;
        this.value = value;
    }

    public ReferenceMapEntryNode(String key, ReferenceNode value) {
        this.key = new ReferenceScalarNode(key);
        this.value = value;
    }

    public ReferenceMapEntryNode(String key, String value) {
        this.key = new ReferenceScalarNode(key);
        this.value = new ReferenceScalarNode(value);
    }

    @Override
    public ReferenceNode getKey() {
        return key;
    }

    @Override
    public ReferenceNode getValue() {
        return value;
    }

    @Override
    public void setRaw(ReferenceNode value) {
        this.value = value;
    }

    @Override
    public List<ReferenceNode> getChildren() {
        return List.of(key, value);
    }

    @Override
    public List<CommentAstNode> getComments() {
        return Collections.emptyList();
    }
}
