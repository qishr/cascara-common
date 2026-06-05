package io.github.qishr.cascara.common.lang.reference;

import io.github.qishr.cascara.common.lang.ast.*;
import io.github.qishr.cascara.common.lang.token.Token;

public abstract class ReferenceNode implements AstNode {
    @Override
    public int getStartLine() { return 0; }
    @Override
    public int getStartColumn() { return 0; }
    @Override
    public int getEndLine() { return 0; }
    @Override
    public int getEndColumn() { return 0; }
    @Override
    public Token getToken() { return null; }
}
