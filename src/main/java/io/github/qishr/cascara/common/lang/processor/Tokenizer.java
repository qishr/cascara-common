package io.github.qishr.cascara.common.lang.processor;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import io.github.qishr.cascara.common.lang.token.Token;
import io.github.qishr.cascara.common.lang.token.TokenType;


public interface Tokenizer<T extends Token> extends Processor {

    /// High-level API: Tokenizes a complete String eagerly.
    default List<T> tokenize(String text) {
        // Fallback or default implementation using the stream approach
        return TokenizerUtils.drain(this, text);
    }

    /// High-level API: Tokenizes an entire InputStream eagerly.
    default List<T> tokenize(InputStream is) {
        return TokenizerUtils.drain(this, is);
    }

    /// Low-level Streaming API: Resets the tokenizer state to read from a String.
    void open(String text);

    /// Low-level Streaming API: Resets the tokenizer state to read from a stream.
    void open(InputStream is);

    /// Low-level Streaming API: Pulls the next token on demand.
    T nextToken();

    default Set<? extends TokenType> getTokenTypes() {
        return Set.of();
    }
}