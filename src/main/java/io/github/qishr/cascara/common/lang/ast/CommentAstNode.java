package io.github.qishr.cascara.common.lang.ast;

public interface CommentAstNode extends AstNode {
    /// The actual comment text without the '#'
    String asString();

    /// The lexeme, including the '#'
    String getRaw();

    /// Useful for CSS or Java modules
    boolean isMultiLine();
}
