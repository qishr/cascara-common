package io.github.qishr.cascara.common.lang.processor;

import java.net.URI;
import java.util.List;

import io.github.qishr.cascara.common.lang.StructuredDocument;
import io.github.qishr.cascara.common.lang.exception.ParserException;
import io.github.qishr.cascara.common.lang.token.Token;

public interface Parser<D extends StructuredDocument, T extends Token> extends Processor {
    D parse(String text) throws ParserException;
    D parse(String text, URI uri) throws ParserException;
    D parse(List<T> tokens) throws ParserException;
    D parse(List<T> tokens, URI uri) throws ParserException;
}
