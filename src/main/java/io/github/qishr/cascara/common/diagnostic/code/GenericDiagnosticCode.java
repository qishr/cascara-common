package io.github.qishr.cascara.common.diagnostic.code;

public enum GenericDiagnosticCode implements DiagnosticCode {
    INFO("INFO-101", "{0}."),

    WARN("WARN-101", "{0}."),
    UNIMPLEMENTED_METHOD("WARN-102", "Unimplemented method: {0}.{1}."),

    ERROR("ERROR-101", "Error: {0}."),
    EXCEPTION("ERROR-102", "Exception: {0}."),
    RUNTIME_EXCEPTION("ERROR-103", "Runtime exception: {0}."),
    NPE("ERROR-104", "Null pointer exception: {0}."),
    UNEXPECTED_NULL("ERROR-105","Unexpected null {0}."),
    INCONSISTENT_STATE("ERROR-106","Inconsistent state."),
    UNSUPPORTED_OPERATION("ERROR-107","Unsupported operation: {0}."),

    IO_ERROR("ERROR-201", "IO error: {0}."),
    INTERRUPT_ERROR("ERROR-301", "Interrupt error: {0}."),

    FORMAT_ERROR("ERROR-401", "Format error: {0}."),
    INVALID_URI("ERROR-402", "Invalid URI: {0}."),
    UNKNOWN_URI_SCHEME("ERROR-403", "Unknown URI scheme: {0}."),
    MALFORMED_BASE64("ERROR-404", "Malformed Base64 payload"),

    NO_RESOURCE_PROVIDER("ERROR-501", "No resource provider.");


    private final String code;
    private final String message;

    GenericDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}