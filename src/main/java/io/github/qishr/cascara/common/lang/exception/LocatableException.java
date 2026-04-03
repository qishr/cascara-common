package io.github.qishr.cascara.common.lang.exception;

import java.net.URI;

public interface LocatableException {
    int getLine();
    int getColumn();
    URI getUri();
    String getMessage();
    String getRawMessage();
}
