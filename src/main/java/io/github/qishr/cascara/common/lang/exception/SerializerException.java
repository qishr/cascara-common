package io.github.qishr.cascara.common.lang.exception;

public class SerializerException extends LocatableException {
    public SerializerException(String m) {
        super(m, 0, 0, null);
    }

    public SerializerException(String m, Throwable t) {
        super(m, t, 0, 0, null);
    }
}
