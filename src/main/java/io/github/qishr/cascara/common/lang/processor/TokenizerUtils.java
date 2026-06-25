package io.github.qishr.cascara.common.lang.processor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.lang.token.Token;

class TokenizerUtils {
    static <T extends Token> List<T> drain(Tokenizer<T> tokenizer, String text) {
        tokenizer.open(text);
        return drainAll(tokenizer);
    }

    static <T extends Token> List<T> drain(Tokenizer<T> tokenizer, InputStream is) {
        tokenizer.open(is);
        return drainAll(tokenizer);
    }

    private static <T extends Token> List<T> drainAll(Tokenizer<T> tokenizer) {
        List<T> tokens = new ArrayList<>();
        T token;
        while ((token = tokenizer.nextToken()) != null) {
            tokens.add(token);
        }
        return tokens;
    }
}