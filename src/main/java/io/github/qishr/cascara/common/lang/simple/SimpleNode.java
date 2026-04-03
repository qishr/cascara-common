package io.github.qishr.cascara.common.lang.simple;

import io.github.qishr.cascara.common.lang.ast.*;
import io.github.qishr.cascara.common.lang.token.Token;

import java.net.URI;

public abstract class SimpleNode implements AstNode {
    @Override
    public int getStartLine() { return 0; }
    @Override
    public int getStartColumn() { return 0; }
    @Override
    public int getEndLine() { return 0; }
    @Override
    public int getEndColumn() { return 0; }
    @Override
    public URI getOriginUri() { return null; }
    @Override
    public Token getToken() { return null; }
}
