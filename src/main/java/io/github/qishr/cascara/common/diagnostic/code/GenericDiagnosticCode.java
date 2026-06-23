package io.github.qishr.cascara.common.diagnostic.code;

public enum GenericDiagnosticCode implements DiagnosticCode {
    INFO("INFO-101", "{0}."),

    WARN("WARN-101", "{0}."),
    UNIMPLEMENTED_METHOD("WARN-102", "Unimplemented method: {0}.{1}."),

    ERROR("ERROR-101", "Error: {0}."),
    UNEXPECTED_NULL("ERROR-102","Unexpected null: {0}"),
    UNEXPECTED_NULL_PARAMETER("ERROR-103","Unexpected null paramter \"{1}\" in {0}"),

    EXCEPTION("ERROR-202", "Exception: {0}."),
    RUNTIME_EXCEPTION("ERROR-203", "Runtime exception: {0}."),
    NPE("ERROR-204", "Null pointer exception: {0}."),
    INCONSISTENT_STATE("ERROR-206","Inconsistent state."),
    UNSUPPORTED_OPERATION("ERROR-207","Unsupported operation: {0}."),

    IO_ERROR("ERROR-301", "IO error: {0}."),
    INTERRUPT_ERROR("ERROR-301", "Interrupt error: {0}."),

    FORMAT_ERROR("ERROR-401", "Format error: {0}."),
    INVALID_URI("ERROR-402", "Invalid URI: {0}."),
    UNKNOWN_URI_SCHEME("ERROR-403", "Unknown URI scheme: {0}."),
    MALFORMED_BASE64("ERROR-404", "Malformed Base64 payload"),
    MESSAGE_FORMATTING_ERROR("ERROR-405", "Problem encountered while formatting message with key {0}: {1}"),
    DIAGNOSTIC_FORMATTING_ERROR("ERROR-406", "Problem encountered while formatting error with code {0}: {1}"),

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