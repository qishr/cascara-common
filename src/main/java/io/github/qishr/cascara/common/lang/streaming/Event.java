package io.github.qishr.cascara.common.lang.streaming;

import io.github.qishr.cascara.common.lang.annotation.Experimental;

@Experimental
public interface Event {
    EventType getType();
    String getContent();
    long getLineNumber();
    long getColumnNumber();
}