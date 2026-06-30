package io.github.qishr.cascara.common.lang.streaming;

import io.github.qishr.cascara.common.lang.annotation.Experimental;

@Experimental
@FunctionalInterface
public interface StreamHandler {
    void onEvent(Event event);
}