package io.github.qishr.cascara.common.diagnostic.code;

public enum LangDiagnosticCode implements DiagnosticCode {
    EXPECTED_STREAM_START("LANG-101", "Expected stream start."),
    EXPECTED_STREAM_END("TOKEN-102", "Expected steam end."),
    UNEXPECTED_STREAM_END("TOKEN-103", "Unexpected stream end.");

    private final String code;
    private final String message;

    LangDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}