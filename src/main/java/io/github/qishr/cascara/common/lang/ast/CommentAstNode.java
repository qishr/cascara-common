package io.github.qishr.cascara.common.lang.ast;

public interface CommentAstNode extends AstNode {
    String asString();     // The actual comment text without the '#'
    String getRaw();       // The lexeme, including the '#'
    boolean isMultiLine(); // Useful for CSS or Java modules
}
