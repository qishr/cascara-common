package io.github.qishr.cascara.common.diagnostic.code;

public enum FileDiagnosticCode implements DiagnosticCode {
    FILE_NOT_FOUND("FILE-101", "File not found: {0}"),
    ACCESS_DENIED("FILE-102", "Access denied"),
    DISK_FULL("FILE-201", "Disk Full");

    private final String code;
    private final String message;

    FileDiagnosticCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public String getCode() { return code; }
    @Override public String getMessage() { return message; }
}