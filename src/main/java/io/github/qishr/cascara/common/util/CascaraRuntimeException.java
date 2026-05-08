package io.github.qishr.cascara.common.util;

public class CascaraRuntimeException extends RuntimeException {
    public CascaraRuntimeException(String message) {
        super(message);
    }

    public CascaraRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
