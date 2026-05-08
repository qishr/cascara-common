package io.github.qishr.cascara.common.lang.processor;

import io.github.qishr.cascara.common.lang.ast.AstNode;

public interface AstConverter<T extends AstNode> extends Processor {
    String toText(AstNode ast);
    T fromAst(AstNode ast);
}
