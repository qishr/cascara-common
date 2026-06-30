package io.github.qishr.cascara.common.lang.processor;

import io.github.qishr.cascara.common.lang.annotation.Beta;
import io.github.qishr.cascara.common.lang.exception.ParserException;
import io.github.qishr.cascara.common.lang.streaming.Event;

import java.lang.AutoCloseable;
import java.util.Iterator;

@Beta
public interface PullParser extends Processor, Iterator<Event>, AutoCloseable {
    /// Advances to the next event in the stream and returns it.
    /// Returns null (or an END_DOCUMENT event) when the stream is exhausted.
    Event next() throws ParserException;

    /// Checks if the parser can continue advancing.
    boolean hasNext() throws ParserException;

}