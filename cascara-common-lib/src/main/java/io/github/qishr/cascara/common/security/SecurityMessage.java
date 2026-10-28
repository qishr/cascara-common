package io.github.qishr.cascara.common.security;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public enum SecurityMessage implements DiagnosticMessage {
    NO_ALGORITHM("SEC-101", "No suitable algorithm found for: {0}"),
    NO_SUCH_ALGORITHM("SEC-102", "No such algorithm: {0}");

    private final String code;
    private final String format;

    SecurityMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}