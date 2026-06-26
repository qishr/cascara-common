package io.github.qishr.cascara.common.diagnostic.code;

public enum LangDiagnosticCode implements DiagnosticCode {
    EXPECTED_STREAM_START("LANG-101", "Expected stream start."),
    EXPECTED_STREAM_END("TOKEN-102", "Expected steam end."),
    UNEXPECTED_STREAM_END("TOKEN-103", "Unexpected stream end."),

    // Serializer
    FAILED_TO_MAP_TYPE("YAML-501", "Failed to map {0} to YAML AST: {1}"),
    FAILED_TO_MAP_AST("YAML-502", "Failed to map YAML AST to {0}: {1}"),

    CLASS_NOT_SERIALIZABLE("YAML-503", "Class {0} is not serializable"),
    FIELD_NOT_ACCESSIBLE("", "Field {0} is not accessible"),
    NO_SUCH_METHOD("", "No such method: {0}"),
    INVOCATION_TARGET_EXCEPTION("", "Method {0} threw an invocation target exception"),
    ILLEGAL_ARGUMENT_EXCEPTION("", "Field {0} threw an illegal argument exception"),
    INSTANTIATION_EXCEPTION("", "Field {0} threw an instantiation exception"),

    EXPECTED_MAP_STRUCTURE("YAML-", "Expected a map structure for class {0}"),
    FAILED_SERIALIZE("YAML-", "Failed to serialize: {0}"),
    FAILED_DESERIALIZE("YAML-", "Failed to deserialize: {0}: {1}."),
    // EXPECTED_YAML_NODE("YAML-", "Expected YamlNode for serializable type: {0}"),
    INCOMPATIBLE_TYPES("YAML-", "Incompatible types: Cannot map {0} to Java type {1}"),
    FAILED_DESERIALIZE_SCALAR("YAML-", "Failed to deserialize scalar to {0}: {1}"),
    UNSUPPORTED_TYPE("YAML-", "Unsupported field type: {0}"),
    EXPECTED_SEQUENCE("YAML-", "Expected a sequence for field: {0}");

    private final String code;
    private final String message;

    LangDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}