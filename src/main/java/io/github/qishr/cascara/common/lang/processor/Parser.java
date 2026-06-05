package io.github.qishr.cascara.common.lang.processor;

import java.util.List;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.token.Token;

public interface Parser<N extends AstNode, T extends Token> extends Processor {
    /// Entry point for parsing a source string.
    ///
    /// @param text The raw text source.
    /// @return The root [AstNode].
    N parse(String text);

    /// Parses the top-level document structure and handles stream boundaries.
    ///
    /// @param tokens A list of tokens representing the tokenized text source.
    /// @return The root [AstNode].
    N parse(List<T> tokens);
}
