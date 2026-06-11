package io.github.qishr.cascara.common.diagnostic.code;

public enum HttpDiagnosticCode implements DiagnosticCode {
    // Infrastructure Errors (Pre-response)
    // NAME_RESOLUTION_FAILED("HTTP-001", 1),
    // CONNECTION_TIMEOUT("HTTP-002", 1),
    // TLS_HANDSHAKE_FAILED("HTTP-003", 1),

    // Standard HTTP Mappings (Dynamic Catch-All or Explicit Enums)
    BAD_REQUEST("HTTP-400", "Bad Request: {0}"),
    FORBIDDEN("HTTP-403", "Forbidden: {0}"),
    NOT_FOUND("HTTP-404", "Not found: {0}"),
    SERVER_ERROR("HTTP-500", "Server error: {0}");

    private final String code;
    private final String message;

    HttpDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }

    /// Factory method to dynamically resolve or generate an HTTP code
    /// straight from a standard network response status code.
    public static String fromStatusCode(int statusCode) {
        return "HTTP-" + statusCode;
    }
}