package io.github.qishr.cascara.common.lang.ast;

import java.net.URI;
import java.util.List;

import io.github.qishr.cascara.common.lang.token.Token;

public interface AstNode {
    int getStartLine();
    int getStartColumn();
    int getEndLine();
    int getEndColumn();
    URI getOriginUri();
    List<? extends AstNode> getChildren();
    List<CommentAstNode> getComments();
    default Token getToken() { return null; }
    default String getString() { return ""; }
}
