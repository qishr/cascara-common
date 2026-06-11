package io.github.qishr.cascara.common.util;

/// @deprecated  As of release 1.1.0
@Deprecated(forRemoval = true)
public class CascaraRuntimeException extends RuntimeException {
    public CascaraRuntimeException(String message) {
        super(message);
    }

    public CascaraRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
