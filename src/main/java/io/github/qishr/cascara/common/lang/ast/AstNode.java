package io.github.qishr.cascara.common.lang.ast;

import java.util.List;

import io.github.qishr.cascara.common.lang.token.Token;

public interface AstNode {
    int getStartLine();
    int getStartColumn();
    int getEndLine();
    int getEndColumn();
    List<? extends AstNode> getChildren();
    List<CommentAstNode> getComments();
    default Token getToken() { return null; }
    default String asString() { return ""; }
}
