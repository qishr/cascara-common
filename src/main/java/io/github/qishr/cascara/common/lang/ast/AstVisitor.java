package io.github.qishr.cascara.common.lang.ast;

/// Defines a visitor pattern interface for traversing the Cascara AST hierarchy.
public interface AstVisitor {
    // // Stream and Document containers
    // void visit(StreamNode node);
    // void visit(DocumentNode node);
    // void visit(DirectiveNode node);

    // Standard structural nodes
    void visit(MapAstNode<?,?> node);
    void visit(MapEntryAstNode<?> node);
    void visit(SequenceAstNode<?> node);
    void visit(ScalarAstNode<?> node);
    // void visit(AliasAstNode node);
    void visit(CommentAstNode node);
    // void visit(AnchorAstNode node);
}