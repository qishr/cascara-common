package io.github.qishr.cascara.common.security.diagnostic;

import io.github.qishr.cascara.common.i18n.LocalizableMessage;

public enum SecurityMessage implements LocalizableMessage {
    NO_ALGORITHM("SEC-101", "No suitable algorithm found for: {0}"),
    UNSUPPORTED_ALGORITHM("SEC-102", "Unsupported algorithm: {0}"),
    UNSUPPORTED_DIGEST("SEC-103", "Unsupported digest: {0}"),
    INVALID_KEY("SEC-104", "Invalid key: {0}"),
    EMPTY_CERT_CHAIN("SEC-105", "Empty certificate chain"),
    EMPTY_TRUST_STORE("SEC-106", "Empty trust store"),
    CERT_PATH_VALIDATION_FAILED("SEC-107", "Certificate path validation failed"),
    ERROR_READING_MANIFEST("SEC-108", "Error reading manifest: {0}"),
    NOT_A_SIGNED_ENTRY("SEC-109", "{0} is not a signed entry"),
    INCORRECT_DIGEST("SEC-110", "Incorrect digest for {0}"),
    INCOMPLETE_SETUP("SEC-111", "Incomplete setup of JAR verifier"),
    MISSING_ATTRIBUTES("SEC-112", "Missing attributes for entry {0} in MANIFEST.MF"),
    MISSING_DIGEST("SEC-113", "Missing digest for {0}"),
    MISSING_KEY_FILE("SEC-114", "Missing key file"),
    EXTRA_KEY_FILE("SEC-115", "Extra key file");

    private final String code;
    private final String format;

    SecurityMessage(String code, String format) {
        this.code = code;
        this.format = format;
    }

    @Override public String getCode() { return code; }
    @Override public String getFormat() { return format; }
}