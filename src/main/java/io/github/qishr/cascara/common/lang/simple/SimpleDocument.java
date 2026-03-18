package io.github.qishr.cascara.common.lang.simple;

import java.net.URI;
import java.util.List;

import io.github.qishr.cascara.common.lang.StructuredDocument;
import io.github.qishr.cascara.common.lang.ast.*;

public final class SimpleDocument extends SimpleNode implements StructuredDocument {
    private URI originUri;
    private URI schemaUri;

    private final SimpleMapNode root;

    public SimpleDocument(SimpleMapNode root) {
        this.root = root;
    }

    public SimpleMapNode getRoot() {
        return root;
    }

    public URI getOriginUri() { return originUri; }
    public void setOriginUri(URI originUri) { this.schemaUri = originUri; }

    public URI getSchemaUri() { return schemaUri; }
    public void setSchemaUri(URI schemaUri) { this.schemaUri = schemaUri; }

    public AstNode get(String key) {
        return root.get(key);
    }

    public String getString(String key) {
        return root.getString(key);
    }

    public MapAstNode<?, ?> getMap(String key) {
        return root.getMap(key);
    }

    public SimpleSequenceNode getSequence(String key) {
        return root.getSequence(key);
    }

    @Override
    public List<? extends AstNode> getChildren() {
        return List.of(root);
    }

    @Override
    public List<CommentAstNode> getComments() {
        return List.of();
    }
}
