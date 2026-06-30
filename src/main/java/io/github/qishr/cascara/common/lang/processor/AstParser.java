package io.github.qishr.cascara.common.lang.processor;

import java.io.InputStream;
import java.util.List;

import io.github.qishr.cascara.common.lang.annotation.Beta;
import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.token.Token;

@Beta
public interface AstParser<N extends AstNode, T extends Token> extends Processor {
    /// Entry point for parsing a source string.
    ///
    /// @param text The raw text source.
    /// @return The root [AstNode].
    N parse(String text);

    /// Entry point for parsing an `InputStream`.
    ///
    /// @param InputStream is An input stream of the raw text source.
    /// @return The root [AstNode].
    N parse(InputStream is);

    /// Primary parsing core driven directly by the Tokenizer interface structure.
    ///
    /// @param tokenizer the tokenizer instance.
    /// @return The root [AstNode].
    N parse(Tokenizer<T> tokenizer);

    /// Entry point for parsing a list of tokens.
    ///
    /// @param tokens A list of tokens representing the tokenized text source.
    /// @return The root [AstNode].
    N parse(List<T> tokens);
}
