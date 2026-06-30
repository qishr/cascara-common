package io.github.qishr.cascara.common.lang.processor;

import java.io.InputStream;

import io.github.qishr.cascara.common.lang.annotation.Beta;
import io.github.qishr.cascara.common.lang.exception.ParserException;
import io.github.qishr.cascara.common.lang.streaming.StreamHandler;

@Beta
public interface PushParser extends Processor {
    /// Eagerly drives the input stream to completion, passing every
    /// structural event encountered directly to the registered handler.
    void parse(InputStream input, StreamHandler handler) throws ParserException;
}