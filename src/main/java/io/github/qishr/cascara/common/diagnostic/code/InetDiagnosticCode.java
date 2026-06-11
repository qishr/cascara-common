package io.github.qishr.cascara.common.diagnostic.code;

public enum InetDiagnosticCode implements DiagnosticCode {
    NETWORK_UNREACHABLE("INET-101", "Network unreachable"),
    CONNECTION_REFUSED("INET-102", "Connection refused: {0}"),
    CONNECTION_TIMEOUT("INET-103", "Connection timeout: {0}"),
    TLS_HANDSHAKE_FAILED("INET-201", "TLS Handshake Failed: {0}");

    private final String code;
    private final String message;

    InetDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}