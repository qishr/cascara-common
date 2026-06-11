package io.github.qishr.cascara.common.diagnostic.code;
public enum DnsDiagnosticCode implements DiagnosticCode {
    UNKNOWN_HOST("DNS-101", "Unknown Host: {0}"),
    TIMEOUT("DNS-102", "DNS Timeout: {0}");

    private final String code;
    private final String message;

    DnsDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}