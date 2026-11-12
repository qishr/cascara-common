package io.github.qishr.cascara.common.security.diagnostic;

import io.github.qishr.cascara.common.diagnostic.message.DiagnosticMessage;

public enum SecurityMessage implements DiagnosticMessage {
    NO_ALGORITHM("SEC-101", "No suitable algorithm found for: {0}"),
    UNSUPPORTED_ALGORITHM("SEC-102", "Unsupported algorithm: {0}"),
    UNSUPPORTED_DIGEST("SEC-103", "Unsupported digest: {0}"),
    INVALID_KEY("SEC-104", "Invalid key: {0}");

    private final String code;
    private final String format;

    SecurityMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}